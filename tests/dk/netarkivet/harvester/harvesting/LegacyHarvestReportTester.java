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
package dk.netarkivet.harvester.harvesting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.LogManager;

import junit.framework.Assert;
import junit.framework.TestCase;

import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.distribute.TestRemoteFile;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.StopReason;
import dk.netarkivet.harvester.harvesting.report.LegacyHarvestReport;
import dk.netarkivet.harvester.harvesting.report.AbstractHarvestReport;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.LogUtils;
import dk.netarkivet.testutils.Serial;
import dk.netarkivet.testutils.TestFileUtils;

/**
 * unit tests for the abstract class AbstractHarvestReport and its concrete
 * implementation {@link LegacyHarvestReport}.
 */
public class LegacyHarvestReportTester extends TestCase {
    public LegacyHarvestReportTester(String s) {
        super(s);
    }

    public void setUp() throws Exception {
        TestRemoteFile.removeRemainingFiles();
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        TestInfo.WORKING_DIR.mkdirs();
        File logs = new File(TestInfo.WORKING_DIR, "logs");
        logs.mkdir();
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR,
                                          TestInfo.WORKING_DIR);
        FileInputStream fis = new FileInputStream(
                "tests/dk/netarkivet/testlog.prop");
        LogManager.getLogManager().reset();
        FileUtils.removeRecursively(TestInfo.LOG_FILE);
        LogManager.getLogManager().readConfiguration(fis);
    }


    public void tearDown() {
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        JMSConnectionMockupMQ.clearTestQueues();
    }


    public void testConstructor() throws IOException {
        //Test null argument
        try {
            new LegacyHarvestReport(null);
            fail("Failed to throw ArgumentNotValid exception on null-arguments "
                 + "to constructor");
        } catch (ArgumentNotValid e) {
            // expected
        }

        //Test parse error
        FileUtils.copyFile(
                TestInfo.INVALID_REPORT_FILE,
                new File(TestInfo.WORKING_DIR, "logs/crawl.log"));
        HeritrixFiles hf = new HeritrixFiles(TestInfo.WORKING_DIR, new JobInfoTestImpl(1L, 1L));
        new LegacyHarvestReport(hf);
        LogUtils.flushLogs(LegacyHarvestReport.class.getName());
        FileAsserts.assertFileContains("Should have log about invalid line",
                                       "FINE: Invalid line in",
                                       TestInfo.LOG_FILE);

        //Test success
        FileUtils.copyFile(
                TestInfo.REPORT_FILE,
                new File(TestInfo.WORKING_DIR, "logs/crawl.log"));
        hf = new HeritrixFiles(TestInfo.WORKING_DIR, new JobInfoTestImpl(1L, 1L));
        AbstractHarvestReport hostReport = new LegacyHarvestReport(hf);

        assertNotNull(
                "A AbstractHarvestReport should have a non-null set of domain names",
                hostReport.getDomainNames());
        assertNotNull(
                "A AbstractHarvestReport should have a non-null number of object counts",
                hostReport.getObjectCount("netarkivet.dk"));
        assertNotNull(
                "A AbstractHarvestReport should have a non-null number of bytes retrieved",
                hostReport.getByteCount("netarkivet.dk"));
    }

    public void testGetDomainNames() throws IOException, FileNotFoundException {

        FileUtils.copyFile(
                TestInfo.REPORT_FILE,
                new File(TestInfo.WORKING_DIR, "logs/crawl.log"));
        HeritrixFiles hf = new HeritrixFiles(TestInfo.WORKING_DIR, new JobInfoTestImpl(1L, 1L));
        AbstractHarvestReport hostReport = new LegacyHarvestReport(hf);

        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(TestInfo.REPORT_FILE)));
        int lineCnt = 0;
        while (reader.readLine() != null) {
            ++lineCnt;
        }
        reader.close();

        if (lineCnt > 1) {
            assertTrue(
                    "Number of domain names in AbstractHarvestReport should be > 0, assuming "
                    + "that the number of lines in the host-reports.txt file is > 1",
                    hostReport.getDomainNames().size() > 0);
        }

        // Number of domain names in AbstractHarvestReport should be less than or equal to
        // the number of lines in the host-reports.txt file (minus 1 , due to header):
        Assert.assertEquals(
                "Number of domain names in AbstractHarvestReport should equal testnumber "
                + TestInfo.NO_OF_TEST_DOMAINS,
                TestInfo.NO_OF_TEST_DOMAINS, //Expected value
                hostReport.getDomainNames().size());

        // Check if set of domain names contains normalized domain name TestInfo.TEST_DOMAIN:
        assertTrue("hostReport.getDomainNames() should contain domain name "
                   + dk.netarkivet.harvester.harvesting.TestInfo.TEST_DOMAIN,
                   hostReport.getDomainNames().contains(
                           dk.netarkivet.harvester.harvesting.TestInfo.TEST_DOMAIN));
    }


    public void testGetObjectCount() {
        AbstractHarvestReport hostReport = createValidHeritrixHostsReport();

        assertEquals(
                "AbstractHarvestReport.getObjectCount(TestInfo.TEST_DOMAIN)) should expected to return "
                + TestInfo.NO_OF_OBJECTS_TEST,
                TestInfo.NO_OF_OBJECTS_TEST, //Expected value
                (long) hostReport.getObjectCount(
                        dk.netarkivet.harvester.harvesting.TestInfo.TEST_DOMAIN));
        assertNull(
                "AbstractHarvestReport.getObjectCount('bibliotek.dk')) expected to return Null",
                hostReport.getObjectCount("bibliotek.dk"));

    }


    public void testGetByteCount() {
        AbstractHarvestReport hostReport = createValidHeritrixHostsReport();

        assertEquals(
                "AbstractHarvestReport.getByteCount(TestInfo.TEST_DOMAIN)) expected to return "
                + TestInfo.NO_OF_BYTES_TEST,
                TestInfo.NO_OF_BYTES_TEST, //Expected value
                (long) hostReport.getByteCount(
                        dk.netarkivet.harvester.harvesting.TestInfo.TEST_DOMAIN));
        assertNull(
                "AbstractHarvestReport.getByteCount('bibliotek.dk')) expected to return Null",
                hostReport.getByteCount("bibliotek.dk"));
    }

    /** Test solution to bugs 391 - hosts report with long values. */
    public void testLongValues() {
        File testFile = TestInfo.LONG_REPORT_FILE;

        FileUtils.copyFile(
                testFile,
                new File(TestInfo.WORKING_DIR, "logs/crawl.log"));
        HeritrixFiles hf = new HeritrixFiles(TestInfo.WORKING_DIR, new JobInfoTestImpl(1L, 1L));
        AbstractHarvestReport hr = new LegacyHarvestReport(hf);
        Long expectedObjectCount = Long.valueOf(2L);
        Long expectedByteCount = new Long(5500000001L);
        assertEquals("Counts should equal input data", expectedObjectCount,
                     hr.getObjectCount("dom.dk"));
        assertEquals("Counts should equal input data", expectedByteCount,
                     hr.getByteCount("dom.dk"));

    }

    /**
     * Test solution to bugs 392 - hosts report with byte counts which add to a
     * long value.
     */
    public void testAddLongValues() {
        File testFile = TestInfo.ADD_LONG_REPORT_FILE;

        FileUtils.copyFile(
                testFile,
                new File(TestInfo.WORKING_DIR, "logs/crawl.log"));
        HeritrixFiles hf = new HeritrixFiles(TestInfo.WORKING_DIR, new JobInfoTestImpl(1L, 1L));

        AbstractHarvestReport hr = new LegacyHarvestReport(hf);
        assertEquals("Counts should equal input data", new Long(2500000000l),
                     hr.getByteCount("nosuchdomain.dk"));
    }

    /** Test stop reason. */
    public void testStopReason() {
        File testFile = TestInfo.STOP_REASON_REPORT_FILE;
        FileUtils.copyFile(
                testFile,
                new File(TestInfo.WORKING_DIR, "logs/crawl.log"));
        HeritrixFiles hf = new HeritrixFiles(TestInfo.WORKING_DIR, new JobInfoTestImpl(1L, 1L));
        
        AbstractHarvestReport hr = new LegacyHarvestReport(hf);
        assertEquals("kb.dk is unfinished",
                     StopReason.DOWNLOAD_UNFINISHED,
                     hr.getStopReason("kb.dk"));
        assertEquals("netarkivet.dk reached byte limit",
                     StopReason.SIZE_LIMIT,
                     hr.getStopReason("netarkivet.dk"));
        assertEquals("statsbiblioteket.dk reached object limit",
                     StopReason.OBJECT_LIMIT,
                     hr.getStopReason("statsbiblioteket.dk"));
        assertEquals("no information about bibliotek.dk",
                     null,
                     hr.getStopReason("bibliotek.dk"));
    }
    
    public void testIDNA() {
        File testFile = TestInfo.IDNA_CRAW_LOG;
        FileUtils.copyFile(
                testFile,
                new File(TestInfo.WORKING_DIR, "logs/crawl.log"));
        HeritrixFiles hf = new HeritrixFiles(TestInfo.WORKING_DIR, new JobInfoTestImpl(1L, 1L));
        AbstractHarvestReport hr = new LegacyHarvestReport(hf);
        boolean disregardSeedUrl = Settings.getBoolean(
                HarvesterSettings.DISREGARD_SEEDURL_INFORMATION_IN_CRAWLLOG); 
        if (disregardSeedUrl) { 
            assertTrue(hr.getByteCount("oelejr.dk") != null);
            assertTrue(hr.getByteCount("østerbrogades-dyrlæger.dk") != null);
            assertTrue(hr.getDomainNames().size() > 2);
        } else {
            assertTrue(hr.getByteCount("ølejr.dk") != null);
            assertTrue(hr.getByteCount("østerbrogades-dyrlæger.dk") != null);
            assertTrue(hr.getDomainNames().size() == 2);
        }
    }
    
    /**
     * Tests object can be serialized and deserialized preserving state.
     *
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void testSerializability()
            throws IOException, ClassNotFoundException {
        File testFile = TestInfo.REPORT_FILE;
        FileUtils.copyFile(
                testFile,
                new File(TestInfo.WORKING_DIR, "logs/crawl.log"));
        HeritrixFiles hf = new HeritrixFiles(TestInfo.WORKING_DIR, new JobInfoTestImpl(1L, 1L));
        AbstractHarvestReport hr = new LegacyHarvestReport(hf);
        AbstractHarvestReport hr2 = Serial.serial(hr);
        assertEquals("Relevant state should be preserved",
                     relevantState(hr), relevantState(hr2));
    }

    private String relevantState(AbstractHarvestReport hhr) {
        String s = "";
        List<String> list = new ArrayList<String>(hhr.getDomainNames());
        Collections.sort(list);
        for (String x : list) {
            s += x + "##" + hhr.getByteCount(x) + "##"
                 + hhr.getObjectCount(x) + "\n";
        }
        return s;
    }

    private AbstractHarvestReport createValidHeritrixHostsReport() {
        File testFile = TestInfo.REPORT_FILE;
        FileUtils.copyFile(
                testFile,
                new File(TestInfo.WORKING_DIR, "logs/crawl.log"));
        HeritrixFiles hf = new HeritrixFiles(TestInfo.WORKING_DIR, new JobInfoTestImpl(1L, 1L));
        return new LegacyHarvestReport(hf);
    }

}
