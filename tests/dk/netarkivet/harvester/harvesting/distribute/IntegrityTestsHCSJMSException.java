/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
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

package dk.netarkivet.harvester.harvesting.distribute;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.security.Permission;
import java.util.ArrayList;
import java.util.logging.LogManager;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.QueueConnection;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.ChannelsTester;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.DataModelTestCase;
import dk.netarkivet.harvester.datamodel.HarvestChannel;
import dk.netarkivet.harvester.datamodel.HarvestDAOUtils;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.harvesting.metadata.MetadataEntry;
import dk.netarkivet.harvester.scheduler.JobDispatcher;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.TestUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import junit.framework.TestCase;

/**
 * An integrity test that tests for how the HarvestControllerClient reacts
 * to the occurrence of an JMSException.
 */
public class IntegrityTestsHCSJMSException extends TestCase{

    TestInfo info = new TestInfo();

    /* The server used for testing */
    HarvestControllerServer hs;
    private SecurityManager originalSM;
    ReloadSettings rs = new ReloadSettings();

    public IntegrityTestsHCSJMSException(String sTestName) {
        super(sTestName);
    }

    public void setUp() {
        rs.setUp();
        FileUtils.removeRecursively(TestInfo.SERVER_DIR);
        TestInfo.WORKING_DIR.mkdirs();
        try {
            TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR, TestInfo.WORKING_DIR);
        } catch (IOFailure e) {
            fail("Could not copy working-files to: " + TestInfo.WORKING_DIR.getAbsolutePath());
        }

        try {
            LogManager.getLogManager().readConfiguration(new FileInputStream(TestInfo.TESTLOGPROP));
        } catch (IOException e) {
            fail("Could not load the testlog.prop file");
        }
        Settings.set(CommonSettings.JMS_BROKER_CLASS,
                     "dk.netarkivet.common.distribute.JMSConnectionSunMQ");
        ChannelsTester.resetChannels();
        HarvestDAOUtils.resetDAOs();
        Settings.set(HarvesterSettings.HARVEST_CONTROLLER_SERVERDIR, TestInfo.SERVER_DIR.getAbsolutePath());
        hs = HarvestControllerServer.getInstance();
        originalSM = System.getSecurityManager();
        SecurityManager manager = new SecurityManager() {
            public void checkPermission(Permission perm) {
                if(perm.getName().equals("exitVM")) {
                    notifyAll();
                    throw new SecurityException("Thou shalt not exit in a unit test");
                }
            }
        };
        System.setSecurityManager(manager);
    }

    /**
     * After test is done close test-objects.
     */
    public void tearDown() {
        if (hs != null) {
            hs.close();
        }
        FileUtils.removeRecursively(TestInfo.SERVER_DIR);
        ChannelsTester.resetChannels();
        HarvestDAOUtils.resetDAOs();
        System.setSecurityManager(originalSM);
        rs.tearDown();
    }

    /**
     * Test that a Harvester will not die immediately a JMSException is received.
     */
    public void testJMSExceptionWhileCrawling() throws Exception {
       if (!TestUtils.runningAs("CSR")) {
                   return;
               }
        // Get the exception handler for the connection
        JMSConnection con = JMSConnectionFactory.getInstance();
        Field queueConnectionField = con.getClass().getSuperclass().getDeclaredField("myQConn");
        queueConnectionField.setAccessible(true);
        QueueConnection qc = (QueueConnection) queueConnectionField.get(con);
        ExceptionListener qel = qc.getExceptionListener();
        //Start a harvest
        Job j = TestInfo.getJob();
        DataModelTestCase.addHarvestDefinitionToDatabaseWithId(
                j.getOrigHarvestDefinitionID());
        JobDAO.getInstance().create(j);
        j.setStatus(JobStatus.SUBMITTED);
        JobDispatcher hDisp = new JobDispatcher(con, HarvestDefinitionDAO.getInstance(), JobDAO.getInstance());
        hDisp.doOneCrawl(j, "test", "test", "test", new HarvestChannel("test", false, true, ""), "unittesters",
                new ArrayList<MetadataEntry>());
        //Trigger the exception handler - should not try to exit
        qel.onException(new JMSException("Some exception"));
        // Wait for harvester to finish and try to exit
        synchronized(this) {
            wait();
        }
        // Should probably now do some tests on the state of the HCS to see
        // that it has finished harvesting but not tried to upload
    }

}
