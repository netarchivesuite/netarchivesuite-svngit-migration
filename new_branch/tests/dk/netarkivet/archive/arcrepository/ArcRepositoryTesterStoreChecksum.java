/* File:    $Id$
* Version: $Revision$
* Date:    $Date$
* Author:  $Author$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.archive.arcrepository;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import junit.framework.TestCase;

import dk.netarkivet.archive.arcrepository.distribute.StoreMessage;
import dk.netarkivet.archive.arcrepositoryadmin.ArchiveStoreState;
import dk.netarkivet.archive.arcrepositoryadmin.UpdateableAdminData;
import dk.netarkivet.archive.bitarchive.distribute.BitarchiveClient;
import dk.netarkivet.archive.bitarchive.distribute.BitarchiveServer;
import dk.netarkivet.common.Settings;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.ChannelsTester;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.TestRemoteFile;
import dk.netarkivet.common.distribute.arcrepository.BitArchiveStoreState;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.MD5;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;


/**
 * Unittests for the ArcRepository class.
 */
public class ArcRepositoryTesterStoreChecksum extends TestCase {

    private UseTestRemoteFile rf = new UseTestRemoteFile();

    private static final File TEST_DIR =
            new File("tests/dk/netarkivet/archive/arcrepository/data/store");

    /**
     * The directory from where we upload the ARC files.
     */
    private static final File ORIGINALS_DIR =
            new File(TEST_DIR, "originals");
    /**
     * The files that are uploaded during the tests and that must be removed
     * afterwards.
     */
    private static final String[] STORABLE_FILES = new String[]{"NetarchiveSuite-store1.arc", "NetarchiveSuite-store2.arc"};


    /* The bitarchive servers we need to communicate with */
    BitarchiveServer archiveServer1;
    BitarchiveServer archiveServer2;

    ArcRepository arcRepos;

    public ArcRepositoryTesterStoreChecksum() {
    }

    protected void setUp() {
        Settings.reload();
        ChannelsTester.resetChannels();
        ServerSetUp.setUp();
        arcRepos = ServerSetUp.getArcRepository();
        rf.setUp();
    }

    protected void tearDown() {
        rf.tearDown();
        ServerSetUp.tearDown();
    }


    /**
     * Tests if the store operation generates and stores a valid checksum in the reference table (AdminData).
     */
    public void testStoreCompletedChecksum() {
        File file = new File(ORIGINALS_DIR, STORABLE_FILES[0]);
        String orgCheckSum = null;
        try {
            orgCheckSum = MD5.generateMD5onFile(file);
        } catch (IOException e) {
            throw new IOFailure("Unexpected IO Failure: ", e);
        }

        StoreMessage msg = new StoreMessage(Channels.getError(), file);
        arcRepos.store(msg.getRemoteFile(), msg);
        UploadWaiting.waitForUpload(file, this);
        String refTableSum = UpdateableAdminData.getUpdateableInstance().getCheckSum(file.getName());
        assertEquals(refTableSum, orgCheckSum);
    }

    /**
     * Tests that Controller.getCheckSum() behaves as expected when using a reference to a non-stored file.
     */
    public void testGetChecksumNotStoredFile() {
        File file = new File(ORIGINALS_DIR, STORABLE_FILES[0]);
        // do nothing with file - e.g. not storing it
        // thus checksum reference table should not contain an entry for the file, i.e. getCheckSum() should return null:
        try {
            UpdateableAdminData.getUpdateableInstance().getCheckSum(file.getName());
            fail("Should throw UnknownID when getting non-existing checksum");
        } catch (UnknownID e) {
            //Expected
        }
    }

    /**
     * Tests if an attempt to store an already uploaded/stored file produces the expected behavior: a PermissionDenied should be thrown,
     * and the original entry in checksum reference table remains unaffected.
     */
    public void testStoreFailedAlreadyUploadedChecksum() {
        File file = null;
        String orgCheckSum = null;
        String storedCheckSum = null;
        try {
            file = new File(ORIGINALS_DIR, STORABLE_FILES[0]);
            try {
                orgCheckSum = MD5.generateMD5onFile(file);
            } catch (IOException e) {
                e.printStackTrace();
                fail("Unexpected IOException thrown at generateMD5onFile()");
            }
            JMSConnection con = JMSConnectionFactory.getInstance();
            StoreMessage msg = new StoreMessage(Channels.getError(), file);
            arcRepos.store(msg.getRemoteFile(), msg);
            UploadWaiting.waitForUpload(file, this);
            String refTableSum = UpdateableAdminData.getUpdateableInstance().getCheckSum(file.getName());
            assertEquals("Stored checksum and reference checksum should be equal", refTableSum, orgCheckSum);
            storedCheckSum = refTableSum;
            // attempting to upload/store the file again:
            msg = new StoreMessage(Channels.getError(), file);
            arcRepos.store(msg.getRemoteFile(), msg);
            UploadWaiting.waitForUpload(file, this);
        } catch (dk.netarkivet.common.exceptions.PermissionDenied e) {
            String refTableSum = UpdateableAdminData.getUpdateableInstance().getCheckSum(file.getName());
            // the checksum stored in reference table (during first store operation) should be unaffected
            // by this second attempt to store the file:
            assertEquals("Stored checksum and reference checksum should be equal", refTableSum, storedCheckSum);
        } catch (IOFailure e) {
            e.printStackTrace();
            fail("Unexpected IOException thrown while trying to re-upload file: " + e);
        }

    }

    /** Check what happens if we're being sent a checksum while uploading.
     * Test for bug #410. */
    public void testStoreChecksumWhileUploading() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, NoSuchFieldException {
        final String correctChecksum = "correct checksum";
        final String ba1Name = "ba1";
        final String ba2Name = "ba2";

        UpdateableAdminData ad = UpdateableAdminData.getUpdateableInstance();

        String arcFileName = "store1a.ARC";
        ArchiveStoreState generalState = new ArchiveStoreState(BitArchiveStoreState.UPLOAD_STARTED);
        ad.addEntry(arcFileName, null, correctChecksum);
        ad.setState(arcFileName, ba1Name, BitArchiveStoreState.UPLOAD_STARTED);
        ad.setState(arcFileName, ba2Name, BitArchiveStoreState.DATA_UPLOADED);

        Method m = ArcRepository.class.getDeclaredMethod("processCheckSum",
                new Class[] { String.class, String.class, String.class, String.class });
        m.setAccessible(true);
        m.invoke(arcRepos, new Object[] { arcFileName,
                                          ba1Name,
                                          correctChecksum,
                                          correctChecksum } );
        assertEquals("Should be in state STORE_COMPLETED after correct checksum",
                BitArchiveStoreState.UPLOAD_COMPLETED,
                ad.getState(arcFileName,  ba1Name));

        arcFileName = "store1b.ARC";

        ad.addEntry(arcFileName, null, correctChecksum);
        ad.setState(arcFileName, ba1Name, BitArchiveStoreState.UPLOAD_STARTED);
        ad.setState(arcFileName, ba2Name, BitArchiveStoreState.DATA_UPLOADED);
        m.invoke(arcRepos, new Object[] { arcFileName,
                                          ba1Name,
                                          correctChecksum,
                                          "wrong checksum" } );
        assertEquals("Should go into UPLOAD_FAILED without outstanding remotefile",
                BitArchiveStoreState.UPLOAD_FAILED,
                ad.getState(arcFileName, ba1Name));


        arcFileName = "NetarchiveSuite-store2.arc";
        ad.addEntry(arcFileName, null, correctChecksum);
        ad.setState(arcFileName, ba1Name, BitArchiveStoreState.UPLOAD_STARTED);
        ad.setState(arcFileName, ba2Name, BitArchiveStoreState.DATA_UPLOADED);
        Field f = ArcRepository.class.getDeclaredField("outstandingRemoteFiles");
        f.setAccessible(true);
        Map<String, RemoteFile> outstandingRemoteFiles =
                (Map<String, RemoteFile>)f.get(arcRepos);
        f = ArcRepository.class.getDeclaredField("connectedBitarchives");
        f.setAccessible(true);
        Map<String, BitarchiveClient> connectedBitarchives =
                (Map<String, BitarchiveClient>)f.get(arcRepos);
        connectedBitarchives.put(ba1Name, BitarchiveClient.getInstance(
                Channels.getAllBa(), Channels.getAnyBa(),
                Channels.getTheBamon()));
        // Have to use a real file here, as startUpload will grab the name
        outstandingRemoteFiles.put(arcFileName, new TestRemoteFile(new File(ORIGINALS_DIR, STORABLE_FILES[1]),
                                                                    false, false,
                                                                    false));
        m.invoke(arcRepos, new Object[] { arcFileName,
                                          ba1Name,
                                          correctChecksum,
                                          "wrong checksum" } );
        assertEquals("Wrong checksum should always result in upload failure",
                BitArchiveStoreState.UPLOAD_FAILED,
                ad.getState(STORABLE_FILES[1], ba1Name));
    }
}