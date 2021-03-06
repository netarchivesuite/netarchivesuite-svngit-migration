/*$Id$
* $Revision$
* $Date$
* $Author$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2011 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.harvester.harvesting;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.harvester.harvesting.metadata.MetadataFileWriter;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;

import junit.framework.TestCase;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;

public class IngestableFilesTester extends TestCase {
    private static final String MSG = "This a test message from IngestableFilesTester";
    
    /* variables used by all tests */
    private Long testJobId = 1L;
    private Long testHarvestId = 2L;
    private Long badJobId = -33L;
    private JobInfo acceptableJobInfoForJobOne = new JobInfoTestImpl(testJobId, testHarvestId);
    private JobInfo acceptableJobInfoForJobTwo = new JobInfoTestImpl(2L, testHarvestId);
    private JobInfo acceptableJobInfoForJob42 = new JobInfoTestImpl(TestInfo.JOB_ID, testHarvestId);
    private JobInfo unacceptableJobInfo = new JobInfoTestImpl(badJobId, testHarvestId);
    private File existingDir = TestInfo.WORKING_DIR;
    private File nonexistingDir = new File(TestInfo.WORKING_DIR, "doesnotexist");
    
    private MoveTestFiles mtf =
        new MoveTestFiles(TestInfo.CRAWLDIR_ORIGINALS_DIR, TestInfo.WORKING_DIR);

    public void setUp() {
        mtf.setUp();
    }

    public void tearDown() {
        mtf.tearDown();
    }

    /**
     * Verify that ordinary construction does not throw Exception.
     * Verify that constructing with nonexisting crawldir or negative jobID fails.
     */
    public void testConstructor() {
        HeritrixFiles OkFiles = new HeritrixFiles(TestInfo.WORKING_DIR, acceptableJobInfoForJobOne);
        new IngestableFiles(OkFiles);
        HeritrixFiles NotOkFiles = new HeritrixFiles(nonexistingDir, acceptableJobInfoForJobOne);
        HeritrixFiles NotOkFilesWithBadJobId = new HeritrixFiles(existingDir, unacceptableJobInfo);
            
        try {
            new IngestableFiles(NotOkFiles);
            fail("IngestableFiles should reject a nonexisting crawldir");
        } catch (ArgumentNotValid e) {
            //Expected
        }
        try {
            new IngestableFiles(NotOkFilesWithBadJobId);
            fail("IngestableFiles should reject a negativ jobID");
        } catch (ArgumentNotValid e) {
            //Expected
        }
    }

    /**
     * Verify that method returns false before metadata has been generated.
     * Verify that method returns false before metadata generation has finished
     * (indicated by setMetadataReady()).
     * Verify that method returns true after metadata generation has finished.
     *
     * Note that disallowed actions concerning metdataReady are tested in another method.
     * Note that rediscovery of metadata is tested in another method.
     */
    public void testGetSetMetadataReady() {
        HeritrixFiles OkFiles = new HeritrixFiles(TestInfo.WORKING_DIR, acceptableJobInfoForJobOne);
        IngestableFiles inf = new IngestableFiles(OkFiles);
        assertFalse("isMetadataReady() should return false before metadata has been generated",
                inf.isMetadataReady());
        assertFalse("isMetadataFailed() should return false before metadata has been generated",
                inf.isMetadataFailed());
        MetadataFileWriter mfw = inf.getMetadataWriter();
        
        writeOneRecord(mfw);
        assertFalse("isMetadataReady() should return false before all metadata has been generated",
                inf.isMetadataReady());
        assertFalse("isMetadataFailed() should return false before all metadata has been generated",
                inf.isMetadataFailed());
        inf.setMetadataGenerationSucceeded(true);
        assertTrue("isMetadataReady() should return true after metadata has been generated",
                inf.isMetadataReady());
        assertFalse("isMetadataFailed() should return false after metadata has been generated",
                inf.isMetadataFailed());
        HeritrixFiles OkFilesTwo = new HeritrixFiles(TestInfo.WORKING_DIR, acceptableJobInfoForJobTwo);
        inf = new IngestableFiles(OkFilesTwo);
        assertFalse("isMetadataReady() should return false before metadata has been generated",
                inf.isMetadataReady());
        assertFalse("isMetadataFailed() should return false before metadata has been generated",
                inf.isMetadataFailed());
        mfw = inf.getMetadataWriter();
        writeOneRecord(mfw);
        assertFalse("isMetadataReady() should return false before all metadata has been generated",
                inf.isMetadataReady());
        assertFalse("isMetadataFailed() should return false before all metadata has been generated",
                inf.isMetadataFailed());
        inf.setMetadataGenerationSucceeded(false);
        assertFalse("isMetadataReady() should return false after metadata has been generated",
                inf.isMetadataReady());
        assertTrue("isMetadataFailed() should return true before all metadata has been generated",
                inf.isMetadataFailed());
    }

    /**
     * Verify that a PermissionDenied is thrown if
     *  - metadata is NOT ready and getMetadataFiles() is called
     *  - metadata IS ready and getMetadataArcWriter is called
     */
    public void testDisallowedActions() {
        HeritrixFiles OkFiles = new HeritrixFiles(TestInfo.WORKING_DIR, acceptableJobInfoForJobOne);
        IngestableFiles inf = new IngestableFiles(OkFiles);
        
        assertCannotGetMetadata(inf);
        
        MetadataFileWriter aw = inf.getMetadataWriter();
        assertCannotGetMetadata(inf);
        writeOneRecord(aw);
        assertCannotGetMetadata(inf);
        inf.setMetadataGenerationSucceeded(true);
        try {
            inf.getMetadataWriter();
            fail("Should reject getMetadataArcWriter() when metadata is ready");
        } catch (PermissionDenied e) {
            //Expected
        }
        try {
            writeOneRecord(aw);
            fail("Should fail to write when metadata is ready");
        } catch (Throwable e) {
            //Expected
        }
        HeritrixFiles OkFilesTwo = new HeritrixFiles(TestInfo.WORKING_DIR, acceptableJobInfoForJobTwo);
        inf = new IngestableFiles(OkFilesTwo);
        
        assertCannotGetMetadata(inf);
        aw = inf.getMetadataWriter();
        assertCannotGetMetadata(inf);
        writeOneRecord(aw);
        assertCannotGetMetadata(inf);
        inf.setMetadataGenerationSucceeded(false);
        try {
            inf.getMetadataWriter();
            fail("Should reject getMetadataArcWriter() when metadata is failed");
        } catch (PermissionDenied e) {
            //Expected
        }
        try {
            writeOneRecord(aw);
            fail("Should fail to write when metadata is failed");
        } catch (Throwable e) {
            //Expected -- cannot tell what aw.write throws when closed.
        }

    }

    /**
     * Fail if inf.getMetadataArcFiles() does not throw PermissionDenied.
     */
    private void assertCannotGetMetadata(IngestableFiles inf) {
        try {
            inf.getMetadataArcFiles();
            fail("Should reject getMetadataArcFiles() when metadata is not ready");
        } catch (PermissionDenied e) {
            //Expected
        }
    }

    /**
     * Verify that IngestableFiles discovers old (final) metadata in the crawldir.
     */
    public void testMetadataRediscovery() throws FileNotFoundException, IOException {
        //Original crawl: write some metadata
        HeritrixFiles OkFiles = new HeritrixFiles(TestInfo.WORKING_DIR, acceptableJobInfoForJobOne);
        IngestableFiles inf = new IngestableFiles(OkFiles);

        MetadataFileWriter aw = inf.getMetadataWriter();
        writeOneRecord(aw);
        inf.setMetadataGenerationSucceeded(true);
        //Now forget about old state:
        inf = new IngestableFiles(OkFiles);
        //Everything should be well:
        assertTrue("Should rediscover old metadata",inf.isMetadataReady());
        boolean found = false;
        for(File f : inf.getMetadataArcFiles()) {
            if(FileUtils.readFile(f).contains(MSG)) {
                found = true;
            }
        }
        assertTrue("Test metadata should be contained in one of the metadata ARC files "
                + "but wasn't found in " + inf.getMetadataArcFiles(),found);
    }

    /**
     * Verify that a non-null ArcWriter is returned.
     */
    public void testGetMetadataArcWriter() {
        HeritrixFiles OkFiles = new HeritrixFiles(TestInfo.WORKING_DIR, acceptableJobInfoForJobOne);
        IngestableFiles inf = new IngestableFiles(OkFiles);
        MetadataFileWriter aw = inf.getMetadataWriter();
        writeOneRecord(aw);
    }

    /**
     * Verify that a file containing data written to the metadata ARCWriter
     * is contained in one the returned files.
     */
    public void testGetMetadataFiles() throws FileNotFoundException, IOException {
        HeritrixFiles OkFiles = new HeritrixFiles(TestInfo.WORKING_DIR, acceptableJobInfoForJobOne);
        IngestableFiles inf = new IngestableFiles(OkFiles);
        MetadataFileWriter aw = inf.getMetadataWriter();
        writeOneRecord(aw);
        inf.setMetadataGenerationSucceeded(true);
        boolean found = false;
        for(File f : inf.getMetadataArcFiles()) {
            if(FileUtils.readFile(f).contains(MSG)) {
                found = true;
            }
        }
        assertTrue("Test metadata should be contained in one of the metadata ARC files "
                + "but wasn't found in " + inf.getMetadataArcFiles(),found);
    }

    public void testMetadataFailure() {
        HeritrixFiles OkFiles = new HeritrixFiles(TestInfo.WORKING_DIR, acceptableJobInfoForJobOne);
        IngestableFiles inf = new IngestableFiles(OkFiles);
        inf.setMetadataGenerationSucceeded(false);
        try {
            inf.getMetadataArcFiles();
            fail("Should not have been allowed to get failed metadata");
        } catch (PermissionDenied e) {
            //expected
        }
        assertTrue("Metadata should be failed",
                inf.isMetadataFailed());
    }

    /**
     * Test that closeOpenFiles closes the right files.
     *
     * @throws Exception
     */
    public void testCloseOpenFiles() throws Exception {
        // These files should end up closed
        File arcsDir = new File(TestInfo.WORKING_DIR, "arcs");
        File[] openFiles = new File[] {
                new File(arcsDir, "test1.arc.open"),
                new File(arcsDir, "test2.arc.gz.open") };
        // These files should be untouched
        File[] nonOpenFiles = new File[] {
                new File(arcsDir, "test3.arcygz.open"),
                new File(arcsDir, "test4.arc"),
                new File(arcsDir, "test5.arcagz"),
                new File(arcsDir, "test6.arcaopen") };
        for (File openFile : openFiles) {
            openFile.createNewFile();
            assertTrue("Open file '" + openFile
                    + "' should exist before calling closeOpenFiles()",
                    openFile.exists());
        }
        for (File nonOpenFile : nonOpenFiles) {
            nonOpenFile.createNewFile();
            assertTrue("Open file '" + nonOpenFile
                    + "' should exist before calling closeOpenFiles()",
                    nonOpenFile.exists());
        }
        
        HeritrixFiles OkFiles42 = new HeritrixFiles(TestInfo.WORKING_DIR, acceptableJobInfoForJob42);
        IngestableFiles inf = new IngestableFiles(OkFiles42);
        
        inf.closeOpenFiles(0);
        for (File openFile1 : openFiles) {
            assertFalse("Open file '" + openFile1
                    + "' should not exist after calling closeOpenFiles()",
                    openFile1.exists());
            final String path = openFile1.getAbsolutePath();
            assertTrue("Open file '" + openFile1
                    + "' should have been closed after calling closeOpenFiles()",
                    new File(path.substring(0, path.length() - 5)).exists());
        }
        for (File nonOpenFile1 : nonOpenFiles) {
            assertTrue("Non-open file '" + nonOpenFile1
                    + "' should exist after calling closeOpenFiles()",
                    nonOpenFile1.exists());
            final String path = nonOpenFile1.getAbsolutePath();
            final String changedPath = path.substring(0, path.length() - 5);
            assertFalse("Changed non-open file '" + changedPath
                    + "' should not exist after calling closeOpenFiles()",
                    new File(changedPath).exists());
        }
    }
    
    /**
     * Writes a single ARC record (containing MSG) and closes the ARCWriter.
     */
    private static void writeOneRecord(MetadataFileWriter aw) {
        try {
            aw.write("test://test.test/test", "text/plain", "0.0.0.0", new Date().getTime(),
                    MSG.getBytes());
        } catch (IOException e) {
            fail("Should have written a test record and closed ARCWriter");
        }
    }
    
}
