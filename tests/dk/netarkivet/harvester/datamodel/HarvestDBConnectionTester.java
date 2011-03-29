/*$Id$
* $Revision$
* $Date$
* $Author$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 2.1 of the License, or (at your option) any later version.
*
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/
package dk.netarkivet.harvester.datamodel;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.LogManager;

import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.DBUtils;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.LogUtils;

/**
 * Test class for the Database utilities in HarvestDBConnection,
 * especially the ones related to backup of the database.
 *
 */
public class HarvestDBConnectionTester extends DataModelTestCase {

    private File logfile = new File("tests/testlogs/netarkivtest.log");


    public HarvestDBConnectionTester(String s) {
        super(s);
    }


    public void setUp() throws Exception {
        super.setUp();
        FileInputStream fis =
            new FileInputStream("tests/dk/netarkivet/testlog.prop");
        LogManager.getLogManager().reset();
        LogManager.getLogManager().readConfiguration(fis);
        fis.close();
        createTestTable();
      }

    public void tearDown() throws Exception {
        super.tearDown();
        try {
            dropTestTable();
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Simple test if HarvestDBConnection.getDBConnection() works or not.
     * Uses Settings.DB_URL set in DataModelTestCase.SetUp()
     */
     public void testGetDBConnection() {
         Connection c = HarvestDBConnection.get();
         assertTrue("Should return non null Connection", c != null);
     }

    /** Check that the connection has the expected setup.
     * @throws SQLException
     */
    public void testAutocommitOn() throws SQLException {
        Connection c = HarvestDBConnection.get();
        try {
            assertEquals(
                    "Connection should have transaction level READ_COMMITTED",
                    Connection.TRANSACTION_READ_COMMITTED,
                    c.getTransactionIsolation());
            assertTrue("Connection should have autocommit on",
                    c.getAutoCommit());
        } finally {
            HarvestDBConnection.release(c);
        }
    }

    /** Check that read locks are released after use.
     * @throws InterruptedException
     */
    public void testLockRelease() throws InterruptedException {
        // Go-ahead ticker
        final int[] state = new int[1];
        final Throwable[] error1 = new Throwable[1];
        Thread t1 = new Thread() {
            public void run() {
                Connection c = HarvestDBConnection.get();
                try {
                    waitForState(0, state);
                    c.setAutoCommit(false);
                    c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                    DBUtils.selectLongList(c, "SELECT domain_id FROM domains");
                    state[0] = 1;
                    waitForState(2, state);
                } catch (Throwable e) {
                    error1[0] = e;
                    state[0] = -1;
                    return;
                } finally {
                    HarvestDBConnection.release(c);
                }

            }
        };
        final Throwable[] error2 = new Throwable[1];
        Thread t2 = new Thread() {
            public void run() {
                Connection c = HarvestDBConnection.get();
                try {
                    waitForState(1, state);
                    c.setAutoCommit(false);
                    PreparedStatement s = c.prepareStatement("DELETE FROM domains WHERE name = ?");
                    s.setString(1, "netarkivet.dk");
                    s.executeUpdate();
                    c.commit();
                    state[0] = 2;
                } catch (Throwable e) {
                    error2[0] = e;
                    state[0] = -1;
                    return;
                } finally {
                    HarvestDBConnection.release(c);
                }
            }
        };
        t1.start();
        t2.start();
        while (t1.isAlive() && t2.isAlive()) {
            Thread.sleep(10);
        }
        if (error1[0] != null) {
            System.out.println(error1[0]);
            error1[0].printStackTrace();
        }
        if (error2[0] != null) {
            System.out.println(error2[0]);
            error2[0].printStackTrace();
        }
        assertEquals("Should have no exception from thread 1",
                     null, error1[0]);
        assertNull("Should have no exception from thread 2",
                   error2[0]);
    }

    static void waitForState(int state, int[] stateref) {
        while (stateref[0] != state && stateref[0] != -1) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // Ignored, just retesting anyway.
            }
        }
        if (stateref[0] == -1) {
            throw new Error("Other thread failed");
        }
    }

    /** check HarvestDBConnection.setStringMaxLength().
     *  Especially, that bug 970 is solved.
     */
    public void testSetStringMaxLength() throws SQLException {
        Object dummyObject = null;
        int id = 1;
        PreparedStatement s = getPreparedStatementForTestingSetStringMaxLength(id, "nameOfOrderxml", "ContentsOfOrderxmldoc");
        int fieldNum = 2;
        String contents = "contents";
        int maxSize = contents.length();

        // Verify, that setStringMaxLength works without warnings, if contents.length() <= maxSize
        // and that storedContents equals to variable 'contents'
        DBUtils.setStringMaxLength(s, fieldNum, contents, maxSize, dummyObject, "fieldname");

        // Check, that no WARNING has been written to the log
        LogUtils.flushLogs(HarvestDBConnection.class.getName());
        FileAsserts.assertFileNotContains("Log should not have given warning as yet",
                logfile, "setStringMaxLength\nWARNING: fieldname");
        // execute query, and retrieve data
        s.execute();
        String storedContents = retrieveStoredString(id);
        assertEquals("storedContents differs from original", contents, storedContents);

        // Verify, that setClobMaxLength issues a warning, if contents.length() > maxSize
        // and that storedContents equals to the first maxSize characters of variable 'contents'
        id=2;
        s = getPreparedStatementForTestingSetStringMaxLength(id, "nameOfOrderxml", "ContentsOfOrderxmldoc");
        maxSize = contents.length() - 2;
        try {
            DBUtils.setStringMaxLength(s, fieldNum, contents, maxSize, dummyObject, "fieldname");
        } catch (PermissionDenied e) {
            fail("Should never throw PermissionDenied exception");
        }
        //  Check, that WARNING has been written to the log
        LogUtils.flushLogs(HarvestDBConnection.class.getName());
        FileAsserts.assertFileContains("Log should have given warning",
                "setStringMaxLength\nWARNING: fieldname", logfile);
        s.execute();
        storedContents = retrieveStoredString(id);
        assertEquals("storedContents differs from original", contents.substring(0,maxSize), storedContents);
}

    /** check HarvestDBConnection.setClobMaxLength().
     * especially, that bug 970 is solved. */
    public void testSetClobMaxLength() throws SQLException {
        Object dummyObject = null;
        int id = 3;
        PreparedStatement s = getPreparedStatementForTestingSetClobMaxLength(id, "nameOfOrderxml", "ContentsOfOrderxmldoc");
        int fieldNum = 3;
        String contents = "contents";
        long maxSize = contents.length();

        // Verify, that setClobMaxLength works without warnings, if contents.length() <= maxSize
        // and that storedContents equals to variable 'contents'
        DBUtils.setClobMaxLength(s, fieldNum, contents, maxSize, dummyObject, "fieldname");

        // Check, that no WARNING has been written to the log
        LogUtils.flushLogs(HarvestDBConnection.class.getName());
        FileAsserts.assertFileNotContains("Log should not have given warning as yet",
                logfile, "setClobMaxLength\nWARNING: fieldname");

        s.execute();
        String storedContents = retrieveStoredClob(id);
        assertEquals("storedContents differs from original", contents, storedContents);

        // Verify, that setClobMaxLength issues a warning, if contents.length() > maxSize
        // and that storedContents equals to the first maxSize characters of variable 'contents'

        id=4;
        s = getPreparedStatementForTestingSetClobMaxLength(id, "nameOfOrderxml", "ContentsOfOrderxmldoc");
        maxSize = contents.length() - 2;
        try {
            DBUtils.setClobMaxLength(s, fieldNum, contents, maxSize, dummyObject, "fieldname");
        } catch (PermissionDenied e) {
            fail("Should never throw PermissionDenied exception");
        }
        // Check, that a WARNING has been written to the log

        LogUtils.flushLogs(HarvestDBConnection.class.getName());
        FileAsserts.assertFileContains("Log should have given warning",
                "setClobMaxLength\nWARNING: fieldname", logfile);
        s.execute();
        storedContents = retrieveStoredClob(id);
        assertEquals("storedContents differs from original", contents.substring(0,(int) maxSize), storedContents);
    }


    public void testCreateTable() throws SQLException {
        deleteTableIfExists("dummy");
        String[] stmts = {
                "CREATE TABLE dummy (id INT)"
        };
        HarvestDBConnection.updateTable("dummy", 1, stmts);
        Connection con = HarvestDBConnection.get();
        try {
            PreparedStatement s = con.prepareStatement("SELECT id FROM dummy");
            s.executeQuery();
            assertEquals(
                    "Newly created table should have version number 1",
                    1,
                    DBUtils.getTableVersion(con, "dummy"));
        } finally {
            HarvestDBConnection.release(con);
        }
    }

    public void testCreateAndUpdateTable() throws SQLException {
        deleteTableIfExists("dummy");

        String[] stmts = {
                "CREATE TABLE dummy (id INT)"
        };
        HarvestDBConnection.updateTable("dummy", 1, stmts);
        String[] stmts2 = {
                "ALTER TABLE dummy ADD new_field INT"
        };
        HarvestDBConnection.updateTable("dummy", 2, stmts2);
        Connection con = HarvestDBConnection.get();
        try {
            PreparedStatement s =
                con.prepareStatement("SELECT id, new_field FROM dummy");
            s.executeQuery();
            assertEquals(
                    "Update table should have version number 2",
                    2,
                    DBUtils.getTableVersion(con, "dummy"));
        } finally {
            HarvestDBConnection.release(con);
        }
    }

    public PreparedStatement getPreparedStatementForTestingSetStringMaxLength(int id, String orderxml, String orderxmldoc)
    throws SQLException {
        Connection c = HarvestDBConnection.get();
        try {
            PreparedStatement s =
                c.prepareStatement("INSERT INTO HarvestDBConnectionTester (id, orderxml, orderxmldoc) "
                        + " VALUES (?, ?, ?)");
            s.setInt(1, id);
            DBUtils.setClobMaxLength(s, 3, orderxmldoc, 64000L, null, "fieldName");
            return s;
        } finally {
            HarvestDBConnection.release(c);
        }
    }

    public PreparedStatement getPreparedStatementForTestingSetClobMaxLength(int id, String orderxml, String orderxmldoc)
    throws SQLException {
        Connection c = HarvestDBConnection.get();
        try {
            PreparedStatement s =
                c.prepareStatement("INSERT INTO HarvestDBConnectionTester (id, orderxml, orderxmldoc) "
                        + " VALUES (?, ?, ?)");
            s.setInt(1, id);
            DBUtils.setStringMaxLength(s, 2, orderxml, 300, null, "fieldName");
            return s;
        } finally {
            HarvestDBConnection.release(c);
        }
    }

    private void createTestTable() throws SQLException {
        // create "HarvestDBConnectionTester" table for testing set*Max methods
        Connection c = HarvestDBConnection.get();
        try {
            PreparedStatement s =
                c.prepareStatement("CREATE TABLE HarvestDBConnectionTester ( "
                        + " id int, "
                        + " orderxml varchar(300) not null, "
                        + " orderxmldoc clob(64M) not null ) ");
            s.execute();
        } finally {
            HarvestDBConnection.release(c);
        }
    }

    private void dropTestTable() throws SQLException {
        //  drop table "HarvestDBConnectionTester" used for testing set*Max methods
        Connection c = HarvestDBConnection.get();
        try {
            PreparedStatement s =
                c.prepareStatement("DROP TABLE HarvestDBConnectionTester");
            s.execute();
        } finally {
            HarvestDBConnection.release(c);
        }
    }

    private String retrieveStoredString(int id) {
        Connection c = HarvestDBConnection.get();
        try {
            return DBUtils.selectStringValue(
                    c,
                    "SELECT orderxml from HarvestDBConnectionTester where id=?",
                    id);
        } finally {
            HarvestDBConnection.release(c);
        }
    }

    private String retrieveStoredClob(int id) {
        Connection c = HarvestDBConnection.get();
        try {
            return DBUtils.selectStringValue(
                    c,
                    "SELECT orderxmldoc from HarvestDBConnectionTester where id=?",
                    id);
        } finally {
            HarvestDBConnection.release(c);
        }
    }

    /**
     * Delete the given table from the table if it exists.
     * Furthermore delete the given table from table 'schemaversions'
     * @param tablename a given table that we want to have deleted
     * @throws SQLException
     */
    private void deleteTableIfExists(String tablename) throws SQLException {
        Connection con = HarvestDBConnection.get();
        try {
            DatabaseMetaData metadata = con.getMetaData();
            ResultSet rs =
                metadata.getTables(null, null, tablename.toUpperCase(), null);
            if (rs.next()) {
                // A table with the given tablename exists in the database
                // So delete it!!
                PreparedStatement s =
                    con.prepareStatement("DROP TABLE " + tablename);
                s.execute();
                // delete tablename from schemaversions
                s = con.prepareStatement(
                "DELETE FROM schemaversions WHERE tablename=?");
                s.setString(1, tablename);
                s.execute();
            }
        } finally {
            HarvestDBConnection.release(con);
        }
    }
}