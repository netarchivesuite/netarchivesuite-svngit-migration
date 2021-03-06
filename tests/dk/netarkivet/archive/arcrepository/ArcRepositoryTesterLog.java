/* File:                 $Id$
 * Revision:         $Revision$
 * Author:                $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import junit.framework.TestCase;

import dk.netarkivet.archive.arcrepository.distribute.StoreMessage;
import dk.netarkivet.archive.arcrepositoryadmin.AdminData;
import dk.netarkivet.archive.arcrepositoryadmin.UpdateableAdminData;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.arcrepository.ReplicaStoreState;
import dk.netarkivet.common.utils.ChecksumCalculator;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.LogUtils;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;


/**
 * Unit test for webarchive API.
 * The logging of webarchive operations is tested.
 */
public class ArcRepositoryTesterLog extends TestCase {
    protected final Logger log = Logger.getLogger(getClass().getName());

    private UseTestRemoteFile rf = new UseTestRemoteFile();

    private static File CONTROLLER_LOG_FILE 
    	= new File("tests/testlogs/netarkivtest.log");

    private static final File TEST_DIR =
            new File("tests/dk/netarkivet/archive/arcrepository/data");

    /**
     * The directory storing the arcfiles in the already existing bitarchive
     * - including credentials and admin-files.
     */
    private static final File ORIGINALS_DIR =
            new File(new File(TEST_DIR, "logging"), "originals");
    
    /**
     * List of files that can be used in the scripts 
     * (content of the ORIGINALS_DIR).
     */
    private static final List<String> FILES =
            Arrays.asList(new String[]{"logging1.ARC",
                                       "logging2.ARC"});

    /**
     * A Controller object.
     */
    ArcRepository arcRepos;

    public ArcRepositoryTesterLog(String sTestName) {
        super(sTestName);
    }

    protected void setUp() throws IOException {
        ServerSetUp.setUp();
        arcRepos = ServerSetUp.getArcRepository();
        FileInputStream fis = new FileInputStream(
        		"tests/dk/netarkivet/testlog.prop");
        LogManager.getLogManager().reset();
        FileUtils.removeRecursively(CONTROLLER_LOG_FILE);
        LogManager.getLogManager().readConfiguration(fis);
        fis.close();
        rf.setUp();
    }

    protected void tearDown() {
        rf.tearDown();
        ServerSetUp.tearDown();
    }

    /**
     * Test logging of store command.
     */
    public void testLogStore() throws Exception {
        String fileName = FILES.get(0).toString();
        //store the file;
        File f = new File(ORIGINALS_DIR, fileName);
        
        UpdateableAdminData adminData = AdminData.getUpdateableInstance();
        adminData.addEntry(f.getName(), new StoreMessage(
                Channels.getThisReposClient(), f), ChecksumCalculator.calculateMd5(
                f));
        adminData.setState(f.getName(),
                Channels.retrieveReplicaChannelFromReplicaId("TWO").getName(),
                ReplicaStoreState.UPLOAD_COMPLETED);
        adminData.setState(f.getName(),
                Channels.retrieveReplicaChannelFromReplicaId("THREE").getName(),
                ReplicaStoreState.UPLOAD_COMPLETED);

        
        StoreMessage msg = new StoreMessage(Channels.getError(), f);
        arcRepos.store(msg.getRemoteFile(), msg);
        UploadWaiting.waitForUpload(f, this);
        //And check for proper logging:
        LogUtils.flushLogs(ArcRepository.class.getName());
        FileAsserts.assertFileContains("Log contains file after storing.",
                fileName, CONTROLLER_LOG_FILE);
        FileAsserts.assertFileContains("Log should contain the words"
                                       + " 'Store started' after storing.",
                "Store started", CONTROLLER_LOG_FILE);
    }
}