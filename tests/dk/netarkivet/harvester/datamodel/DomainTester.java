/*$Id$
* $Revision$
* $Author$
* $Date$
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
package dk.netarkivet.harvester.datamodel;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.utils.IteratorUtils;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.harvester.webinterface.DomainDefinition;
import dk.netarkivet.testutils.CollectionAsserts;
import dk.netarkivet.testutils.CollectionUtils;


public class DomainTester extends DataModelTestCase {
    public DomainTester(String sTestName) {
        super(sTestName);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test setters and getters with correct parameters
     */
    public void testSetAndGet() {
        Domain wd =  Domain.getDefaultDomain(TestInfo.DOMAIN_NAME);

        wd.addSeedList(TestInfo.seedlist);

        DomainConfiguration newcfg = new DomainConfiguration("Deep", wd,
                                                             Arrays.asList(new SeedList[]{TestInfo.seedlist}), new ArrayList<Password>());

        newcfg.setOrderXmlName(TestInfo.ORDER_XML_NAME);
        newcfg.setMaxObjects(10);
        newcfg.setMaxRequestRate(11);

        wd.addConfiguration(newcfg);
        wd.setDefaultConfiguration(newcfg.getName());

        assertEquals("Expecting name we asked for", TestInfo.DOMAIN_NAME,
                     wd.getName());

        // Retrieve the configuration
        DomainConfiguration cfg = wd.getConfiguration("Deep");

        assertEquals("Expecting value just set", "Deep", cfg.getName());
        assertEquals("Expecting value just set", TestInfo.ORDER_XML_NAME,
                     cfg.getOrderXmlName());
        assertEquals("Expecting value just set", 10, cfg.getMaxObjects());
        assertEquals("Expecting value just set", 11, cfg.getMaxRequestRate());
        assertEquals("Expecting value just set", TestInfo.seedlist.getName(),
                     ((Named)cfg.getSeedLists().next()).getName());
    }

    /**
     * Test setters with incorrect parameters
     */
    public void testSetAndGetArgumentNotValid() {
        try {
            Domain.getDefaultDomain("");
            fail("Empty string not a valid argument");
        } catch (ArgumentNotValid e) {
            // expected
        }



        try {
            Domain.getDefaultDomain(null);
            fail("Null not a valid argument");
        } catch (ArgumentNotValid e) {
            // expected
        }

        Domain wd = Domain.getDefaultDomain(TestInfo.DOMAIN_NAME);

        try {
            wd.addConfiguration(null);
            fail("null not a valid argument");
        } catch (ArgumentNotValid e) {
            // expected
        }

        try {
            wd.addSeedList(null);
            fail("null not a valid argument");
        } catch (ArgumentNotValid e) {
            // expected
        }

        try {
            wd.addPassword(null);
            fail("null not a valid argument");
        } catch (ArgumentNotValid e) {
            // expected
        }

        try {
            wd.setDefaultConfiguration(null);
            fail("null not a valid argument");
        } catch (ArgumentNotValid e) {
            // expected
        }

        try {
            wd.setDefaultConfiguration("Default");
            fail("No configuration has yet been added");
        } catch (UnknownID e) {
            // expected
        }

        /* Test invalid configuration parameters */
        DomainConfiguration cfg = new DomainConfiguration("test",
                                                          TestInfo.getDefaultDomain(),
                                                          Arrays.asList(new SeedList[]{TestInfo.seedlist}),
                                                          new ArrayList<Password>());

        try {
            cfg.setOrderXmlName("");
            fail("Empty string not a valid argument");
        } catch (ArgumentNotValid e) {
            // expected
        }

        try {
            cfg.setOrderXmlName(null);
            fail("Null not a valid argument");
        } catch (ArgumentNotValid e) {
            // expected
        }

        try {
            cfg.setMaxObjects(-2);
            fail("argument must be >-1");
        } catch (ArgumentNotValid e) {
            // expected
        }

        try {
            cfg.setMaxRequestRate(-1);
            fail("Argument must be non negativ");
        } catch (ArgumentNotValid e) {
            // expected
        }

        try {
            cfg.addSeedList(null);
            fail("Null not a valid argument");
        } catch (ArgumentNotValid e) {
            // expected
        }
    }

    /**
     * Verify that configurations with unknown seedlists are rejected by Domain
     *
     * author: SSC
     */
    public void testUnknownSeedList() {
        try {
            Domain wd = Domain.getDefaultDomain(TestInfo.DOMAIN_NAME);
            wd.addSeedList(TestInfo.seedlist);

            DomainConfiguration cfg1 = TestInfo.getDefaultConfig(wd);
            cfg1.addSeedList(new SeedList("Unknown-seedlist", TestInfo.SEEDS1));
            wd.addConfiguration(cfg1);
            fail("The seedlist is unknown");
        } catch (UnknownID e) {
            // expected
        }
    }

    /**
     * Verify that removing a seedlist used by a configuration is rejected
     *
     * author: SSC
     */
    public void testSeedListRemoved() {
        Domain wd = Domain.getDefaultDomain(TestInfo.DOMAIN_NAME);
        wd.addSeedList(TestInfo.seedlist);

        DomainConfiguration cfg1 = TestInfo.getDefaultConfig(wd);
        wd.addConfiguration(cfg1);

        try {
            wd.removeSeedList("Default-seeds");
            fail("The seedlist is in use by one of the configurations");
        } catch (PermissionDenied e) {
            // expected
        }
    }

    /**
     * Verify that adding a seedlist with an already used name fails
     *
     * author: SSC
     */
    public void testDuplicateSeedListName() {
        Domain wd = Domain.getDefaultDomain(TestInfo.DOMAIN_NAME);
        wd.addSeedList(TestInfo.seedlist);

        try {
            wd.addSeedList(TestInfo.seedlist);
            fail("Not allowed to add Seeds with the same name twice");
        } catch (PermissionDenied e) {
            // expected
        }
    }

    /**
     * Verify that adding a configuration with an already used name fails
     *
     * author: SSC
     */
    public void testDuplicateCfgName() {
        Domain wd = Domain.getDefaultDomain(TestInfo.DOMAIN_NAME);
        wd.addSeedList(TestInfo.seedlist);

        DomainConfiguration cfg1 = TestInfo.getDefaultConfig(wd);
        wd.addConfiguration(cfg1);

        try {
            DomainConfiguration cfg2 = TestInfo.getDefaultConfig(wd);
            wd.addConfiguration(cfg2);
        } catch (PermissionDenied e) {
            // expected
        }
    }

    /**
     * Test removal of a configuration.
     * Removing the default configuration is not allowed. At least one cfg must
     * always exists.
     *
     * author: SSC
     */
    public void testRemoveConfiguration() {
        DomainDAO dao = DomainDAO.getInstance();
        Domain wd = Domain.getDefaultDomain(TestInfo.DOMAIN_NAME);
        wd.addSeedList(TestInfo.seedlist);

        DomainConfiguration cfg1 = TestInfo.getDefaultConfig(wd);

        DomainConfiguration cfg2 = new DomainConfiguration("Another", wd,
                                                           Arrays.asList(new SeedList[]{TestInfo.seedlist}), new ArrayList<Password>());
        cfg1.setOrderXmlName(TestInfo.ORDER_XML_NAME);
        cfg2.setOrderXmlName(TestInfo.ORDER_XML_NAME);
        wd.addConfiguration(cfg1);
        wd.addConfiguration(cfg2);
        dao.create(wd);
        String defaultcfgName = Settings.get(Settings.DOMAIN_DEFAULT_CONFIG);
        assertEquals("The default configuration does not change when adding new configs",
                     wd.getDefaultConfiguration().getName(), defaultcfgName);

        try {
            wd.removeConfiguration(defaultcfgName);
            fail("It is not allowed to remove the default configuration");
        } catch (PermissionDenied e) {
            // expected
        }

        // check it is possible to change the default config and remove
        assertEquals("The first configuration added becomes the default",
                     wd.getDefaultConfiguration().getName(), defaultcfgName);
        wd.setDefaultConfiguration("Another");
        dao.update(wd);
        assertEquals("New default", wd.getDefaultConfiguration().getName(),
                     "Another");
        wd.removeConfiguration(defaultcfgName);
    }


    /** Test that we can update a configuration
     * @throws Exception
     */
    public void testUpdateConfiguration() throws Exception {
        Domain d = Domain.getDefaultDomain(TestInfo.DEFAULTNEWDOMAINNAME);
        DomainConfiguration conf = d.getDefaultConfiguration();
        final List<SeedList> seedlists = Arrays.asList(new SeedList[] {
            d.getSeedList(Settings.get(Settings.DEFAULT_SEEDLIST)) });
        try {
            DomainConfiguration conf2 = new DomainConfiguration(TestInfo.CONFIGURATION_NAMEJOB4,
                                                                d, seedlists, new ArrayList<Password>());
            d.updateConfiguration(conf2);
            fail("Should not be able to update non-existing config");
        } catch (UnknownID e) {
            // expected
        }
        d.addPassword(TestInfo.password);
        DomainConfiguration conf2 =
                new DomainConfiguration(conf.getName(),
                                        d, seedlists, Arrays.asList( new Password[] { TestInfo.password } ));
        d.updateConfiguration(conf2);
        DomainConfiguration conf3 = d.getDefaultConfiguration();
        assertEquals("Default config must now equals new config",
                     conf2, conf3);
        assertFalse("New and old config should not be the same",
                    conf.equals(conf3));
    }

    /** Test that we can add a new seedlist. */
    public void testAddSeedList() {
        Domain d = Domain.getDefaultDomain(TestInfo.DEFAULTNEWDOMAINNAME);
        assertFalse("New seedlist must not exist beforehand",
                    d.hasSeedList(TestInfo.SEEDLISTNAME));
        d.addSeedList(new SeedList(TestInfo.SEEDLISTNAME,
                                   TestInfo.SEEDS1));
        assertTrue("New seedlist should exist afterwards",
                   d.hasSeedList(TestInfo.SEEDLISTNAME));
        // Note that getSeedList adds a newline.  Is that reasonable?
        String expected = TestInfo.SEEDS1;
        String result = d.getSeedList(TestInfo.SEEDLISTNAME).getSeedsAsString();
        BufferedReader expectedReader = new BufferedReader(new StringReader(expected));
        BufferedReader resultsReader = new BufferedReader(new StringReader(result));
        String s;
        try {
            while ((s = expectedReader.readLine()) != null) {
                assertEquals("New seedlist should have correct contents", s, resultsReader.readLine());
            }
        } catch (IOException e1) {
            fail("New seedlist should have correct contents");
        }
        // Also check that we can't add an existing seed list
        try {
            d.addSeedList(new SeedList(TestInfo.SEEDLISTNAME,
                                       TestInfo.SEEDS2));
            fail("Should not be allowed to re-add a seedlist");
        } catch (PermissionDenied e) {
            // Expected
        }
        // Confirm that failed add didn't change stuff
        expected = TestInfo.SEEDS1;
        result = d.getSeedList(TestInfo.SEEDLISTNAME).getSeedsAsString();
        expectedReader = new BufferedReader(new StringReader(expected));
        resultsReader = new BufferedReader(new StringReader(result));
        try {
            while ((s = expectedReader.readLine()) != null) {
                assertEquals("Failed add should not have changed contents", s, resultsReader.readLine());
            }
        } catch (IOException e1) {
            fail("Failed add should not have changed contents");
        }
    }

    public void testGetSeedList() {
        Domain wd = Domain.getDefaultDomain(TestInfo.DOMAIN_NAME);
        wd.addSeedList(TestInfo.seedlist);

        try {
            wd.getSeedList(null);
            fail("An empty name should not return a seedlist");
        } catch (ArgumentNotValid e) {
            // expected
        }

        try {
            wd.getSeedList("");
            fail("An empty name should not return a seedlist");
        } catch (ArgumentNotValid e) {
            // expected
        }

        try {
            wd.getSeedList("unknown-name");
            fail("An unknown seedlist name should not return a seedlist");
        } catch (UnknownID e) {
            // expected
        }

        // verify that it is possible to retrive the added list
        SeedList seedlist = wd.getSeedList("Default-seeds");
        assertNotNull("valid id should return valid list", seedlist);
    }

    public void testUpdateSeedList() {
        Domain d = Domain.getDefaultDomain(TestInfo.DEFAULTNEWDOMAINNAME);
        try {
            d.updateSeedList(new SeedList(TestInfo.SEEDLISTNAME,
                                          TestInfo.SEEDS1));
            fail("Should not be allowed to update non-existing seedlist");
        } catch (UnknownID e) {
            // Expected
        }
        d.addSeedList(new SeedList(TestInfo.SEEDLISTNAME,
                                   TestInfo.SEEDS1));

        String expected = TestInfo.SEEDS1;
        String result = d.getSeedList(TestInfo.SEEDLISTNAME).getSeedsAsString();
        BufferedReader expectedReader = new BufferedReader(new StringReader(expected));
        BufferedReader resultsReader = new BufferedReader(new StringReader(result));
        String s;
        try {
            while ((s = expectedReader.readLine()) != null) {
                assertEquals("Seed list contents should be correct before update", s, resultsReader.readLine());
            }
        } catch (IOException e1) {
            fail("Seed list contents should be correct before update");
        }

        d.updateSeedList(new SeedList(TestInfo.SEEDLISTNAME,
                                      TestInfo.SEEDS2));

        expected = TestInfo.SEEDS1;
        result = d.getSeedList(TestInfo.SEEDLISTNAME).getSeedsAsString();
        expectedReader = new BufferedReader(new StringReader(expected));
        resultsReader = new BufferedReader(new StringReader(result));
        try {
            while ((s = expectedReader.readLine()) != null) {
                assertEquals("Seed list contents should be updatede", s, resultsReader.readLine());
            }
        } catch (IOException e1) {
            fail("Seed list contents should be updated");
        }

        assertFalse("Domain should not have unknown seedlist",
                    d.hasSeedList(TestInfo.SEEDLISTNAME_JOB4));

    }

    /**
     * Removing the last seed list is not allowed. At least one seed list
     * must always exists.
     *
     * author: SSC
     */
    public void testRemoveSeedList() {
        Domain wd = Domain.getDefaultDomain(TestInfo.DOMAIN_NAME);

        try {
            wd.removeSeedList(Settings.get(Settings.DEFAULT_SEEDLIST));
            fail("Removing last seedlist not allowed");
        } catch (PermissionDenied e) {
            //Expected
        }

        // verify that it is possible at all to remove seeds
        wd.addSeedList(TestInfo.seedlist2);
        wd.removeSeedList(TestInfo.seedlist2.getName());
    }

    /**
     * Check that it is possible to retrieve all configurations
     */
    public void testGetAllConfigurations() {
        // Create some configurations to retrieve
        Domain wd = Domain.getDefaultDomain(TestInfo.DOMAIN_NAME);
        wd.addSeedList(TestInfo.seedlist);

        DomainConfiguration cfg1 = TestInfo.getDefaultConfig(wd);
        wd.addConfiguration(cfg1);

        DomainConfiguration cfg2 = new DomainConfiguration("Deep", wd,
                                                           Arrays.asList(new SeedList[]{TestInfo.seedlist}), new ArrayList<Password>());
        wd.addConfiguration(cfg2);

        Iterator cfglist = wd.getAllConfigurations();

        DomainConfiguration cfg0 = (DomainConfiguration) cfglist.next();
        DomainConfiguration cfgA = (DomainConfiguration) cfglist.next();
        DomainConfiguration cfgB = (DomainConfiguration) cfglist.next();
        assertFalse("Only 3 configurations expected", cfglist.hasNext());

        assertNotNull("valid config expected", cfg0);
        assertNotNull("valid config expected", cfgA);
        assertNotNull("valid config expected", cfgB);
        assertFalse("The configs must be different",
                    cfgA.getName().equals(cfgB.getName()));
    }


    //add the information about the domain owner
    public void testAddOwnerInfo() {
        //get information about the domain owner
        Date d = new Date();
        DomainOwnerInfo ownerInfo = new DomainOwnerInfo(d, "DomainOwnerInfo");

        Domain dom = Domain.getDefaultDomain("itu.dk");
        dom.addOwnerInfo( ownerInfo);
        assertNotNull("Should NOT be null", ownerInfo.getInfo());
        assertEquals("expecting value", "DomainOwnerInfo", ownerInfo.getInfo());

    }

    //test invalid values
    public void testAddOwnerInfoInvalidParams() {

        Domain dom = Domain.getDefaultDomain("itu.dk");

        try {
            dom.addOwnerInfo( null);
            fail("not be null");
        } catch (ArgumentNotValid e) { //
        }

    }

    //get all the historical data about the domain
    public void testGetHistoricalData() {
        //get he historical data
        Date date = new Date();
        Domain domain = Domain.getDefaultDomain("itu.dk");
        DomainHistory domainHistory = domain.getHistory();

        Long harvestID = new Long(1234);

        HarvestInfo harvestinfo = new HarvestInfo(harvestID, "bar", "foo", date,
                                                  42, 1,
                                                  StopReason.DOWNLOAD_COMPLETE);
        domainHistory.addHarvestInfo(harvestinfo);

        assertNotNull("expect not null", domainHistory.getHarvestInfo());
        assertTrue("at least one info expected",
                   domainHistory.getHarvestInfo().hasNext());

        assertEquals("Expecting prev. set size", 42,
                     (domainHistory.getHarvestInfo().next()).getSizeDataRetrieved());
    }

/*
    public void testgetHistoricalDataInvalidParams() {
        Domain domain = Domain.getDefaultDomain("itu.dk");

        try {
            DomainHistory domainHistory = new DomainHistory(null);
            fail("Parameter not be null");
        } catch (ArgumentNotValid e) {
            //expected
        }
    }
*/
    //test by adding  informations to Harvest
    public void testAddHarvestInfo() {
        //Domain domain = new Domain("itu.dk");
        DomainHistory domainHistory = new DomainHistory();

        Date date = new Date();

        Long harvestID = new Long(1234);

        HarvestInfo harvestinfo = new HarvestInfo(harvestID, "bar", "foo", date, 1L, 1L, StopReason.DOWNLOAD_COMPLETE);
        domainHistory.addHarvestInfo(harvestinfo);

        HarvestInfo harvesInfo_2 = domainHistory.getHarvestInfo().next();

        assertEquals("Should have the same date", harvesInfo_2.getDate(),
                     date);
        assertEquals("Should have the same harvestID",
                     harvesInfo_2.getHarvestID(), harvestID);
    }

    //test the reason why harvest stopped
    public void testSetStopReason() {
        Long harvestID = new Long(1234);
        StopReason stopReason = StopReason.DOWNLOAD_COMPLETE;

        Date date = new Date();

        HarvestInfo harvestinfor = new HarvestInfo(harvestID, "bar", "foo", date, 1L, 1L, stopReason);

        assertEquals("Output should give the same",
                     StopReason.DOWNLOAD_COMPLETE, harvestinfor.getStopReason());
    }

    //test the number of obtained URLs
    public void testSetCountObjectRetrieved() {
        Long harvestID = new Long(1234);

        Date date = new Date();

        HarvestInfo harvestinfor = new HarvestInfo(harvestID, "bar", "foo", date, 1L, 1000L, StopReason.DOWNLOAD_COMPLETE);
        assertEquals("should give the same", 1000L,
                     harvestinfor.getCountObjectRetrieved());
    }

    //test total amount of data download from the domain
    public void testSetSizeDataRetrieved() {
        Long harvestID = new Long(1234);

        Date date = new Date();

        HarvestInfo harvestinfor = new HarvestInfo(harvestID, "bar", "foo", date, 1000L, 1L, StopReason.DOWNLOAD_COMPLETE);

        assertEquals("Should be the same", 1000L,
                     harvestinfor.getSizeDataRetrieved());
    }


    //test create, update and load for the domains
    public void testLoadAndVerifyAllDomainCreated() {
        DomainDAO dao = DomainDAO.getInstance();
        //Date date = new Date();
        IngestDomainList ingestDList = new IngestDomainList();

        assertFalse("Domains should not exist until created", dao.exists("jp.dk"));
        assertFalse("Domains should not exist until created", dao.exists("bb.dk"));

        File domainfile = new File(TestInfo.TEMPDIR, TestInfo.DOMAIN_LIST);
        ingestDList.updateDomainInfo(domainfile, null,
        		new Locale("en"));

        assertTrue("Domains should exist after updating domain info",
                   dao.exists("jp.dk"));
        assertTrue("Domains should exist", dao.exists("borsen.dk"));

        Domain myDomain = dao.read("borsen.dk");
        String domainName = myDomain.getName();

        assertEquals("the right domain name", "borsen.dk", domainName);
    }


    //test invalid values with constructor
    public void testSetAndGetInvalidValues() {
        Date date = new Date();

        try {
            new HarvestInfo(null, "foo", "bar", date, 1L, 1L, StopReason.DOWNLOAD_COMPLETE);

            fail("parameters should not be null");
        } catch (ArgumentNotValid e) { //expected
        }

        try {
            new HarvestInfo(new Long(1L), null, "bar", date, 1L, 1L, StopReason.DOWNLOAD_COMPLETE);

            fail("parameters should not be null");
        } catch (ArgumentNotValid e) { //expected
        }

        try {
            new HarvestInfo(new Long(1L), "foo", null, date, 1L, 1L, StopReason.DOWNLOAD_COMPLETE);

            fail("parameters should not be null");
        } catch (ArgumentNotValid e) { //expected
        }

        try {
            new HarvestInfo(new Long(1L), "foo", "bar", null, 1L, 1L, StopReason.DOWNLOAD_COMPLETE);

            fail("parameters should not be null");
        } catch (ArgumentNotValid e) { //expected
        }

        try {
            new HarvestInfo(new Long(1L), "foo", "bar", date, -1L, 1L, StopReason.DOWNLOAD_COMPLETE);
            fail("One of the parameters should not be negative");
        } catch (ArgumentNotValid e) { //expected
        }

        try {
            new HarvestInfo(new Long(1L), "foo", "bar", date, 1L, -1L, StopReason.DOWNLOAD_COMPLETE);
            fail("One of the parameters should not be negative");
        } catch (ArgumentNotValid e) { //expected
        }

        try {
            new HarvestInfo(new Long(1L), "foo", "bar", date, 1L, 1L, null);
            fail("parameters should not be null");
        } catch (ArgumentNotValid e) { //expected
        }

    }

    /** Test that getting a new domain actually does that.
     * @throws Exception
     */
    public void testGetDefaultDomain() throws Exception {
        Domain d = Domain.getDefaultDomain("foo.dk");
        assertNotNull("Default domain should be obtainable", d);
        assertEquals("Domain name for default domain should be right",
                     "foo.dk", d.getName());
        DomainConfiguration conf = d.getDefaultConfiguration();
        assertNotNull("Configuration for default domain should not be nulL",
                      conf);
        assertTrue("Configuration should have a seedlist",
                   conf.getSeedLists().hasNext());
        SeedList seedlist =
                d.getSeedList((conf.getSeedLists().next()).getName());
        assertNotNull("Default seedlist should exist", seedlist);
        assertEquals("Default seedlist should contain the domain",
                     "http://www.foo.dk", seedlist.getSeedsAsString().trim());
        Domain d1 = Domain.getDefaultDomain("1.2.3.4");
        assertNotNull("Default domain for IP should be obtainable", d1);
        seedlist =
                d1.getSeedList((conf.getSeedLists().next()).getName());
        assertNotNull("Default seedlist should exist", seedlist);
        assertEquals("Default seedlist should contain the domain",
                     "http://1.2.3.4", seedlist.getSeedsAsString().trim());
    }

    /** Test that we can add a password
     * @throws Exception
     */
    public void testAddPassword() throws Exception {
        Domain d = Domain.getDefaultDomain(TestInfo.DEFAULTNEWDOMAINNAME);
        assertFalse("Password should not exist at start",
                    d.hasPassword(TestInfo.PASSWORD_NAME));
        d.addPassword(TestInfo.password);
        assertTrue("Password should exist after adding",
                   d.hasPassword(TestInfo.PASSWORD_NAME));
        assertEquals("Password added should be the same as found",
                     TestInfo.password, d.getPassword(TestInfo.PASSWORD_NAME));
        try {
            d.addPassword(TestInfo.password);
            fail("Should not be able to add password twice");
        } catch (PermissionDenied e) {
            // Expected
        }
    }

    public void testUpdatePassword() throws Exception {
        Domain d = Domain.getDefaultDomain(TestInfo.DEFAULTNEWDOMAINNAME);
        assertFalse("Password should not exist at start",
                    d.hasPassword(TestInfo.PASSWORD_NAME));
        try {
            d.updatePassword(TestInfo.password);
            fail("Should not be able to update missing password");
        } catch (UnknownID e) {
            // Expected
        }
        d.addPassword(TestInfo.password);
        assertTrue("Password should exist after adding",
                   d.hasPassword(TestInfo.PASSWORD_NAME));
        assertEquals("Password added should be the same as found",
                     TestInfo.password, d.getPassword(TestInfo.PASSWORD_NAME));
        Password p2 = new Password(TestInfo.PASSWORD_NAME, "Secret comment!",
                                   "www.snort.dk", "backdoor", "cracker", "letmein");
        d.updatePassword(p2);
        assertEquals("New password should be found instead of old",
                     p2, d.getPassword(TestInfo.PASSWORD_NAME));
        assertFalse("Old and new password should not be the same",
                    p2.equals(TestInfo.password));
    }

    /** Test that we can remove an existing password.
     @throws Exception
     */
    public void testRemovePassword() throws Exception {
        Domain d = Domain.getDefaultDomain(TestInfo.DEFAULTNEWDOMAINNAME);
        d.addPassword(TestInfo.password);
        assertTrue("Password should exist after adding",
                   d.hasPassword(TestInfo.PASSWORD_NAME));
        DomainConfiguration conf = d.getDefaultConfiguration();
        conf.addPassword(TestInfo.password);
        try {
            d.removePassword(TestInfo.PASSWORD_NAME);
            fail("Should not be allowed to remove password in use");
        } catch (PermissionDenied e) {
            // Expected
        }
        conf.removePassword(TestInfo.PASSWORD_NAME);
        d.removePassword(TestInfo.PASSWORD_NAME);
        assertFalse("Password should be gone after removePassword",
                    d.hasPassword(TestInfo.PASSWORD_NAME));
        try {
            d.removePassword(TestInfo.PASSWORD_NAME);
            fail("Should not be able to remove unknown password");
        } catch (UnknownID e) {
            // Expected
        }
    }

    /** Test setting and getting the regular expressions used for exclusions */
    public void testSetCrawlerTraps() {
        Domain d = Domain.getDefaultDomain("dr.dk");
        assertEquals("Crawler traps should return empty list if not defined",
                     d.getCrawlerTraps(), Collections.<String>emptyList());

        List<String> definedregexps = new ArrayList<String>();
        definedregexps.add(".*dr\\.dk.*/.*\\.cgi");
        definedregexps.add(".*statsbiblioteket\\.dk/gentofte.*");
        d.setCrawlerTraps(definedregexps);
        List<String> foundregexps = d.getCrawlerTraps();
        assertEquals("Crawler traps should be remembered as given",
                     definedregexps, foundregexps);

        // Test that a crawlertrap-regexp only consisting of whitespace is not considered
        // as a valid crawlertrap-regexp and not stored

        definedregexps = new ArrayList<String>();
        definedregexps.add(" ");
        d.setCrawlerTraps(definedregexps);
        assertEquals("Crawler traps containing only whitespace should not be considered as a valid crawler-trap",
                     0, d.getCrawlerTraps().size());

        // Whitespace is not removed from a crawler-trap containing other characters than whitespace.
        definedregexps = new ArrayList<String>();
        definedregexps.add("http://valid crawlertrap ");
        d.setCrawlerTraps(definedregexps);
        assertEquals("Leading and trailing whitespace should be conserved in a regexp containing other characters than whitespace",
                     definedregexps, d.getCrawlerTraps());

        try {
            d.setCrawlerTraps(null);
            fail("Expected error on null argument");
        } catch (ArgumentNotValid e) {
            //expected
        }
    }

    /** Test that we have a sensible regexp for checking validity of domain
     * names.
     *
     * @throws Exception
     */
    public void testIsValidDomainName() throws Exception {
        assertFalse("Multidot should not be valid",
                    Domain.isValidDomainName("foo.bar.dk"));
        assertFalse("Multidot should not be valid",
                    Domain.isValidDomainName(".bar.dk"));
        assertFalse("Multidot should not be valid",
                    Domain.isValidDomainName("foo.bar."));
        assertFalse("Strange TLDs should not be valid",
                    Domain.isValidDomainName("foo.bar"));
        assertFalse("Singledot should not be valid",
                    Domain.isValidDomainName(".dk"));
        assertFalse("Ending in dot should not be valid",
                    Domain.isValidDomainName("foo."));
        assertFalse("Nodot should not be valid",
                    Domain.isValidDomainName("dk"));
        assertTrue("Danish characters should be valid",
                   Domain.isValidDomainName("æøåÆØÅëËüÜéÉ.dk"));
        // The following command will extract all non-LDH chars from
        // a domain list:
        //  sed 's/\(.\)/\1\n/g;' <dk-domains-10102005.utf-8.txt | grep -v '[abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.-]' | sort -u
        assertTrue("Characters from the domain list should be legal",
                   Domain.isValidDomainName("åäæéöøü.dk"));
        assertTrue("Raw IP numbers should be legal",
                   Domain.isValidDomainName("192.168.0.1"));
        assertFalse("Mixed IP/DNS names should not be legal",
                    Domain.isValidDomainName("foo.1"));
        assertTrue("DNS names starting with numbers should be legal",
                   Domain.isValidDomainName("1.dk"));
        assertFalse("Temporarily enabled domain names should eventually not be valid",
                    Domain.isValidDomainName("foo.aspx"));
        assertFalse("Temporarily enabled domain names should eventually not be valid",
                    Domain.isValidDomainName("bar.d"));
    }

    public void testDomainNameFromHostname() throws Exception {
        Map<String,String> hostnameToDomainname = new HashMap<String, String>();
        // Normal hostnames
        hostnameToDomainname.put("foo.dk", "foo.dk");
        hostnameToDomainname.put("smurf.bar.com", "bar.com");
        hostnameToDomainname.put("x.y.baz.aero", "baz.aero");
        hostnameToDomainname.put("a.dk", "a.dk");
        // Two part host names
        hostnameToDomainname.put("bbc.co.uk", "bbc.co.uk");
        hostnameToDomainname.put("news.bbc.co.uk", "bbc.co.uk");
        hostnameToDomainname.put("bl.uk", "bl.uk");
        hostnameToDomainname.put("www.bl.uk", "bl.uk");
        // IP-addresses and IP-like hostnames
        hostnameToDomainname.put("1.dk", "1.dk");
        hostnameToDomainname.put("192.168.0.dk", "0.dk");
        hostnameToDomainname.put("192.160.1.2.dk", "2.dk");
        hostnameToDomainname.put("192.168.0.3", "192.168.0.3");
        // Illegal hostnames
        hostnameToDomainname.put("foo.d", null);
        hostnameToDomainname.put("dk", null);
        hostnameToDomainname.put(".dk", null);
        hostnameToDomainname.put("dk.", null);
        hostnameToDomainname.put("[].dk", null);
        hostnameToDomainname.put("192.168.0", null);
        hostnameToDomainname.put("192.168.0.", null);
        hostnameToDomainname.put("3.192.168.0.5", null);

        for (Map.Entry<String,String> entry : hostnameToDomainname.entrySet()) {
            String domainName = Domain.domainNameFromHostname(entry.getKey());
            assertEquals("Domain name should be correctly calculated for " + entry.getKey(),
                         entry.getValue(), domainName);
            if (entry.getValue() != null) {
                assertTrue("Domain name calculated from " + entry.getKey() + " must be a legal domain name",
                           Domain.isValidDomainName(domainName));
            } else {
                assertFalse("Should not get null domain name from legal domainname " + entry.getKey(),
                            Domain.isValidDomainName(entry.getKey()));
            }
        }
    }

    /** Tests for assignment 4.1.1: Adding alias information in database */
    public void testAliasGettersAndSetters() {
        //Create Domain fnord.dk
        DomainDefinition.createDomains("fnord.dk");

        DomainDAO dao = DomainDAO.getInstance();
        Domain d = dao.read("netarkivet.dk");
        Date before = new Date();
        assertNull("Should have no alias info before setting", d.getAliasInfo());

        // Check that non-public setter doesn't force an update of
        // lastAliasUpdate
        Date date = new GregorianCalendar(2005,9,8).getTime();
        d.setAliasInfo(new AliasInfo("netarkivet.dk", "fnord.dk", date));
        assertEquals("Should have set alias after rawSetAliasInfo",
                     "fnord.dk", d.getAliasInfo().getAliasOf());
        assertEquals("Should have set lastUpdate to given date",
                     date, d.getAliasInfo().getLastChange());
        // Check that setting alias to the same as before does not
        // force an update of lastAliasUpdate
        d.updateAlias("fnord.dk");
        assertNotNull("Should have updated lastUpdate on non-changing set",
                      d.getAliasInfo().getLastChange());

        // Try an actual alias setting
        d.updateAlias("kb.dk");
        dao.update(d);
        assertEquals("Should have alias set", "kb.dk", d.getAliasInfo().getAliasOf());
        assertNotNull("Should have lastupdate set", d.getAliasInfo().getLastChange());
        Date after = new Date();
        assertFalse("Should have lastUpdate set within time being set",
                    d.getAliasInfo().getLastChange().after(after) ||
                    d.getAliasInfo().getLastChange().before(before));
        Date setTo = d.getAliasInfo().getLastChange();
        // Sleep for long enough that we can distinguish Date objects before
        // and after
        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            fail("Interrupted during short sleep, remaining tests uncertain");
        }
        // Test error scenarios: Unknown domain, transitivity
        try {
            d.updateAlias("fnord2.dk");
            fail("Should fail on non-existing domain");
        } catch (UnknownID e) {
            // Expected
        }
        assertEquals("Should have alias unchanged after error",
                     "kb.dk", d.getAliasInfo().getAliasOf());
        assertEquals("Should have lastAliasUpdate unchanged after error",
                     setTo, d.getAliasInfo().getLastChange());

        // Test transitivity
        // Test that we cannot alias
        // statsbiblioteket.dk -> netarkivet.dk -> kb.dk when
        // netarkivet.dk -> kb.dk is already established
        Domain d2 = dao.read("statsbiblioteket.dk");
        try {
            d2.updateAlias("netarkivet.dk");
            d = dao.read("netarkivet.dk");
            fail("Should not allow transitive (to aliased) alias - "
                 + d + " is aliased to " + d.getAliasInfo().getAliasOf());
        } catch (IllegalState e) {
            // Expected
        }
        assertEquals("Should have alias unchanged after error",
                     "kb.dk", d.getAliasInfo().getAliasOf());
        assertEquals("Should have lastAliasUpdate unchanged after error",
                     setTo, d.getAliasInfo().getLastChange());

        // Test that we cannot alias
        // netarkivet.dk -> kb.dk -> statsbiblioteket.dk when
        // netarkivet.dk -> kb.dk is already established
        Domain d3 = dao.read("kb.dk");
        try {
            d3.updateAlias("statsbiblioteket.dk");
            fail("Should not allow transitive alias (from father) - "
                 + dao.getAliases(d3.getName()) + " are aliases of " + d3);
        } catch (IllegalState e) {
            // Expected
        }
        assertEquals("Should have alias unchanged after error",
                     "kb.dk", d.getAliasInfo().getAliasOf());
        assertEquals("Should have lastAliasUpdate unchanged after error",
                     setTo, d.getAliasInfo().getLastChange());

        // Check that we cannot alias a domain to itself.
        try {
            d.updateAlias(d.getName());
            fail("Should not be allowed to be aliased to itself");
        } catch (IllegalState e) {
            // Expected
        }

        // Check that we can set the domain to point to another (i.e. we're
        // not just blocking all updates after one has been set).
        d.updateAlias("statsbiblioteket.dk");
        assertEquals("Domain should set other alias",
                     "statsbiblioteket.dk", d.getAliasInfo().getAliasOf());
        assertTrue("Should have lastUpdate set within time being set",
                   d.getAliasInfo().getLastChange().after(after));

        // Check that we can unset alias
        d.updateAlias(null);
        assertNull("Domain should now not be alias", d.getAliasInfo());
    }

    /** Tests that bug 891 and 971 is fixed:
     *  Automatic sorting of configurations and seed-lists om domain-page.
     *  Fixed by adding new Domain methods that return sorted lists:
     *  getAllConfigurationsAsSortedList(),
     *  getAllSeedListsAsSortedList(),
     *  getAllPasswordsAsSortedList()
     *  Note these methods uses Locale to sort according to language
     */
    public void testGetSortedSeedlistAndDomainConfigurationsAndPasswords() {
        Domain d = Domain.getDefaultDomain(TestInfo.DEFAULTNEWDOMAINNAME);
        Locale dkLocale = new Locale("da");
        Locale ukLocale = new Locale("en");
        Locale origDefault;

        // save original default locale value for later checks (should stay the same)
        origDefault = Locale.getDefault();

        // now contains one seedlist named defaultseeds
        List<SeedList> allSeeds = d.getAllSeedListsAsSortedList(Locale.ENGLISH);
        SeedList s0 = allSeeds.get(0);
        assertTrue("Only one seedlist should exist now", allSeeds.size() == 1);
        List<Password> allPasswords = d.getAllPasswordsAsSortedList(Locale.ENGLISH);
        assertTrue("No passwords should exist now", allPasswords.size() == 0);
        List<DomainConfiguration> allConfigs = d.getAllConfigurationsAsSortedList(Locale.ENGLISH);
        assertTrue("Only one DomainConfiguration should exist now", allConfigs.size() == 1);
        // check default locale value
        assertTrue("At 1. check Locale default value has change from " + origDefault.getLanguage() + " to " + Locale.getDefault().getLanguage(), Locale.getDefault().getLanguage() == origDefault.getLanguage());


        SeedList s1 = new SeedList("Plidderpladder", "http://plidder");
        SeedList s2 = new SeedList("ny liste", "http://plidder");
        SeedList s3 = new SeedList("Anden liste", "http://plidder");
        SeedList s4 = new SeedList("Abe liste", "http://plidder");
        SeedList s5 = new SeedList("abe liste", "http://plidder");
        SeedList s6 = new SeedList("åse liste", "http://plidder");
        SeedList s7 = new SeedList("Åse liste", "http://plidder");
        SeedList s8 = new SeedList("ø liste", "http://plidder");
        SeedList s9 = new SeedList("Æble liste", "http://plidder");
        d.addSeedList(s1); d.addSeedList(s2); d.addSeedList(s3);
        d.addSeedList(s4); d.addSeedList(s5); d.addSeedList(s6);
        d.addSeedList(s7); d.addSeedList(s8); d.addSeedList(s9);

        // Now we check, that DK sequence is "abe liste", "Abe liste", "Anden liste", "defaultseeds", "ny liste", "Plidderpladder"
        //                                   "Æble liste", "ø liste", "åse liste", "Åse liste"
        allSeeds = d.getAllSeedListsAsSortedList(dkLocale);

        // Easier way to test contents of a list.  --LC
        CollectionAsserts.assertListEquals("List of seeds should have expected content, in order",
                                           allSeeds,
                                           s5.getName(), s4.getName(), s3.getName(),
                                           "defaultseeds", s2.getName(), s1.getName());

        Iterator<SeedList> iterator = allSeeds.iterator();
        // # 1 to retrieve must be the seedlist named "abe liste"
        SeedList testSeed = (SeedList) iterator.next();
        assertTrue("1. entry must named '" + s5.getName() + "' but was: " + testSeed.getName(),
                testSeed.getName().equals(s5.getName()));
        // # 2 to retrieve must be the seedlist named "Abe liste"
        testSeed = (SeedList) iterator.next();
        assertTrue("2. entry must named '" + s4.getName() + "', but was: " + testSeed.getName(),
                testSeed.getName().equals(s4.getName()));
        // # 3 to retrieve must be the seedlist named "Anden liste"
        testSeed = (SeedList) iterator.next();
        assertTrue("3. entry must named '" + s3.getName() + "', but was: " + testSeed.getName(), testSeed.getName().equals(s3.getName()));
        // # 4 to retrieve must be the seedlist named "defaultseeds"
        testSeed = (SeedList) iterator.next();
        assertTrue("4. entry must named 'defaultseeds', but was:" + testSeed.getName(), testSeed.getName().equals("defaultseeds"));
        // # 5 to retrieve must be the seedlist named "ny liste"
        testSeed = (SeedList) iterator.next();
        assertTrue("5. entry must named '" + s2.getName() + "', but was:" + testSeed.getName(), testSeed.getName().equals(s2.getName()));
        // # 6 to retrieve must be the seedlist named "PlidderPladder"
        testSeed = (SeedList) iterator.next();
        assertTrue("6. entry must named '" + s1.getName() + "', but was:" + testSeed.getName(), testSeed.getName().equals(s1.getName()));
        // # 7 to retrieve must be the seedlist named "Æble liste"
        testSeed = (SeedList) iterator.next();
        assertTrue("7. entry must named '" + s9.getName() + "', but was:" + testSeed.getName(), testSeed.getName().equals(s9.getName()));
        // # 8 to retrieve must be the seedlist named "ø liste"
        testSeed = (SeedList) iterator.next();
        assertTrue("8. entry must named '" + s8.getName() + "', but was:" + testSeed.getName(), testSeed.getName().equals(s8.getName()));
        // # 9 to retrieve must be the seedlist named "åse liste"
        testSeed = (SeedList) iterator.next();
        assertTrue("9. entry must named '" + s6.getName() + "', but was:" + testSeed.getName(), testSeed.getName().equals(s6.getName()));
        // # 10 to retrieve must be the seedlist named "Åse liste"
        testSeed = (SeedList) iterator.next();
        assertTrue("10. entry must named '" + s7.getName() + "', but was:" + testSeed.getName(), testSeed.getName().equals(s7.getName()));
        // check default locale value
        assertTrue("At 2. check Locale default value has change from " + origDefault.getLanguage() + " to " + Locale.getDefault().getLanguage(), Locale.getDefault().getLanguage() == origDefault.getLanguage());

        // Now we check, that UK sequence is "abe liste", "Abe liste", "Æble liste", "Anden liste", "åse liste", "Åse liste","ny liste",
        //                                   "ø liste", "Plidderpladder"
        allSeeds = d.getAllSeedListsAsSortedList(ukLocale);
        iterator = allSeeds.iterator();
        // # 1 to retrieve must be the seedlist named "abe liste"
        testSeed = (SeedList) iterator.next();
        assertTrue("1. entry must named '" + s5.getName() + "' but was: " + testSeed.getName(),
                testSeed.getName().equals(s5.getName()));
        // # 2 to retrieve must be the seedlist named "Abe liste"
        testSeed = (SeedList) iterator.next();
        assertTrue("2. entry must named '" + s4.getName() + "', but was: " + testSeed.getName(),
                testSeed.getName().equals(s4.getName()));
        // # 3 to retrieve must be the seedlist named "Æble liste"
        testSeed = (SeedList) iterator.next();
        assertTrue("3. entry must named '" + s9.getName() + "', but was:" + testSeed.getName(), testSeed.getName().equals(s9.getName()));
        // # 4 to retrieve must be the seedlist named "Anden liste"
        testSeed = (SeedList) iterator.next();
        assertTrue("4. entry must named '" + s3.getName() + "', but was: " + testSeed.getName(), testSeed.getName().equals(s3.getName()));
        // # 5 to retrieve must be the seedlist named "åse liste"
        testSeed = (SeedList) iterator.next();
        assertTrue("5. entry must named '" + s6.getName() + "', but was:" + testSeed.getName(), testSeed.getName().equals(s6.getName()));
        // # 6 to retrieve must be the seedlist named "Åse liste"
        testSeed = (SeedList) iterator.next();
        assertTrue("6. entry must named '" + s7.getName() + "', but was:" + testSeed.getName(), testSeed.getName().equals(s7.getName()));
        // # 7 to retrieve must be the seedlist named "defaultseeds"
        testSeed = (SeedList) iterator.next();
        assertTrue("7. entry must named 'defaultseeds', but was:" + testSeed.getName(), testSeed.getName().equals("defaultseeds"));
        // # 8 to retrieve must be the seedlist named "ny liste"
        testSeed = (SeedList) iterator.next();
        assertTrue("8. entry must named '" + s2.getName() + "', but was:" + testSeed.getName(), testSeed.getName().equals(s2.getName()));
        // # 9 to retrieve must be the seedlist named "PlidderPladder"
        testSeed = (SeedList) iterator.next();
        assertTrue("9. entry must named '" + s1.getName() + "', but was:" + testSeed.getName(), testSeed.getName().equals(s1.getName()));
        // # 10 to retrieve must be the seedlist named "ø liste"
        testSeed = (SeedList) iterator.next();
        assertTrue("10. entry must named '" + s8.getName() + "', but was:" + testSeed.getName(), testSeed.getName().equals(s8.getName()));
        // check default locale value
        assertTrue("At 3. check Locale default value has change from " + origDefault.getLanguage() + " to " + Locale.getDefault().getLanguage(), Locale.getDefault().getLanguage() == origDefault.getLanguage());



        // Testing getAllConfigurationsAsSortedList()
        // Assuming currentconfig has name: "defaultconfig"
        DomainConfiguration defaultConfig = d.getDefaultConfiguration();
        assertTrue("defaultConfig must have name 'defaultconfig', not " + defaultConfig.getName(),
                defaultConfig.getName().equals("defaultconfig"));
        // Add three more configurations
        d.addConfiguration(cloneConfigWithNewName(defaultConfig, "SvendBent"));
        d.addConfiguration(cloneConfigWithNewName(defaultConfig, "gurli"));
        d.addConfiguration(cloneConfigWithNewName(defaultConfig, "cirkus"));
        d.addConfiguration(cloneConfigWithNewName(defaultConfig, "åse"));
        d.addConfiguration(cloneConfigWithNewName(defaultConfig,"ø"));
        d.addConfiguration(cloneConfigWithNewName(defaultConfig,"Åse"));
        d.addConfiguration(cloneConfigWithNewName(defaultConfig, "abe"));
        d.addConfiguration(cloneConfigWithNewName(defaultConfig, "æble"));

        // Now we check DK sorting, that sequence is "abe", "cirkus", "defaultconfig", "gurli", "SvendBent", "æble"
        //                                           "ø", "åse", "Åse"
        assertTrue("#1 element must be named 'abe', and not: " + d.getAllConfigurationsAsSortedList(dkLocale).get(0).getName(),
                d.getAllConfigurationsAsSortedList(dkLocale).get(0).getName().equals("abe"));
        assertTrue("#2 element must be named 'cirkus', and not: " + d.getAllConfigurationsAsSortedList(dkLocale).get(1).getName(),
                d.getAllConfigurationsAsSortedList(dkLocale).get(1).getName().equals("cirkus"));
        assertTrue("#3 element must be named 'defaultconfig', and not: " + d.getAllConfigurationsAsSortedList(dkLocale).get(2).getName(),
                d.getAllConfigurationsAsSortedList(dkLocale).get(2).getName().equals("defaultconfig"));
        assertTrue("#4 element must be named 'gurli', and not: " + d.getAllConfigurationsAsSortedList(dkLocale).get(3).getName(),
                d.getAllConfigurationsAsSortedList(dkLocale).get(3).getName().equals("gurli"));
        assertTrue("#5 element must be named 'SvendBent', and not: " + d.getAllConfigurationsAsSortedList(dkLocale).get(4).getName(),
                d.getAllConfigurationsAsSortedList(dkLocale).get(4).getName().equals("SvendBent"));
        assertTrue("#6 element must be named 'æble', and not: " + d.getAllConfigurationsAsSortedList(dkLocale).get(5).getName(),
                d.getAllConfigurationsAsSortedList(dkLocale).get(5).getName().equals("æble"));
        assertTrue("#7 element must be named 'ø', and not: " + d.getAllConfigurationsAsSortedList(dkLocale).get(6).getName(),
                d.getAllConfigurationsAsSortedList(dkLocale).get(6).getName().equals("ø"));
        assertTrue("#8 element must be named 'åse', and not: " + d.getAllConfigurationsAsSortedList(dkLocale).get(7).getName(),
                d.getAllConfigurationsAsSortedList(dkLocale).get(7).getName().equals("åse"));
        assertTrue("#9 element must be named 'Åse', and not: " + d.getAllConfigurationsAsSortedList(dkLocale).get(8).getName(),
                d.getAllConfigurationsAsSortedList(dkLocale).get(8).getName().equals("Åse"));
        // check default locale value
        assertTrue("At 4. check Locale default value has change from " + origDefault.getLanguage() + " to " + Locale.getDefault().getLanguage(), Locale.getDefault().getLanguage() == origDefault.getLanguage());

        // Now we check UK sorting, that sequence is "abe", "æble", "åse", "Åse", "cirkus", "defaultconfig", "gurli", "ø", "SvendBent"
        assertTrue("#1 element must be named 'abe', and not: " + d.getAllConfigurationsAsSortedList(ukLocale).get(0).getName(),
                d.getAllConfigurationsAsSortedList(ukLocale).get(0).getName().equals("abe"));
        assertTrue("#2 element must be named 'æble', and not: " + d.getAllConfigurationsAsSortedList(ukLocale).get(1).getName(),
                d.getAllConfigurationsAsSortedList(ukLocale).get(1).getName().equals("æble"));
        assertTrue("#3 element must be named 'åse', and not: " + d.getAllConfigurationsAsSortedList(ukLocale).get(2).getName(),
                d.getAllConfigurationsAsSortedList(ukLocale).get(2).getName().equals("åse"));
        assertTrue("#4 element must be named 'Åse', and not: " + d.getAllConfigurationsAsSortedList(ukLocale).get(3).getName(),
                d.getAllConfigurationsAsSortedList(ukLocale).get(3).getName().equals("Åse"));
        assertTrue("#5 element must be named 'cirkus', and not: " + d.getAllConfigurationsAsSortedList(ukLocale).get(4).getName(),
                d.getAllConfigurationsAsSortedList(ukLocale).get(4).getName().equals("cirkus"));
        assertTrue("#6 element must be named 'defaultconfig', and not: " + d.getAllConfigurationsAsSortedList(ukLocale).get(5).getName(),
                d.getAllConfigurationsAsSortedList(ukLocale).get(5).getName().equals("defaultconfig"));
        assertTrue("#7 element must be named 'gurli', and not: " + d.getAllConfigurationsAsSortedList(ukLocale).get(6).getName(),
                d.getAllConfigurationsAsSortedList(ukLocale).get(6).getName().equals("gurli"));
        assertTrue("#8 element must be named 'SvendBent', and not: " + d.getAllConfigurationsAsSortedList(ukLocale).get(7).getName(),
                d.getAllConfigurationsAsSortedList(ukLocale).get(7).getName().equals("SvendBent"));
        assertTrue("#9 element must be named 'ø', and not: " + d.getAllConfigurationsAsSortedList(ukLocale).get(8).getName(),
                d.getAllConfigurationsAsSortedList(ukLocale).get(8).getName().equals("ø"));
        // check default locale value
        assertTrue("At 5. check Locale default value has change from " + origDefault.getLanguage() + " to " + Locale.getDefault().getLanguage(), Locale.getDefault().getLanguage() == origDefault.getLanguage());

        // Testing getAllPasswordsAsSortedList
        Password p1 = createDefaultPassword("defaultpassword");
        d.addPassword(p1);
        Password p2 = clonePasswordWithNewName(p1, "mitPassword");
        d.addPassword(p2);
        Password p3 = clonePasswordWithNewName(p1, "ditPassword");
        d.addPassword(p3);
        Password p4 = clonePasswordWithNewName(p1, "HansPassword");
        d.addPassword(p4);
        Password p5 = clonePasswordWithNewName(p1, "AAseassword");
        d.addPassword(p5);
        Password p6 = clonePasswordWithNewName(p1, "øPassword");
        d.addPassword(p6);
        Password p7 = clonePasswordWithNewName(p1, "ÅsePassword");
        d.addPassword(p7);
        Password p8 = clonePasswordWithNewName(p1, "abePassword");
        d.addPassword(p8);
        Password p9 = clonePasswordWithNewName(p1, "æblePassword");
        d.addPassword(p9);
        Password p10 = clonePasswordWithNewName(p1, "åsePassword");
        d.addPassword(p10);

        // Correct sequence in DK: "abePassword" "defaultpassword", "ditPassword", "HansPassword", "mitPassword", "æblePassword", "øPassword"
        //                         "AAseassword", "åsePassword", "ÅsePassword"
        assertTrue("#1 element must be named 'abePassword', and not: " + d.getAllPasswordsAsSortedList(dkLocale).get(0).getName(),
                d.getAllPasswordsAsSortedList(dkLocale).get(0).getName().equals("abePassword"));
        assertTrue("#2 element must be named 'defaultpassword', and not: " + d.getAllPasswordsAsSortedList(dkLocale).get(1).getName(),
                d.getAllPasswordsAsSortedList(dkLocale).get(1).getName().equals("defaultpassword"));
        assertTrue("#3 element must be named 'ditPassword', and not: " + d.getAllPasswordsAsSortedList(dkLocale).get(2).getName(),
                d.getAllPasswordsAsSortedList(dkLocale).get(2).getName().equals("ditPassword"));
        assertTrue("#4 element must be named 'HansPassword', and not: " + d.getAllPasswordsAsSortedList(dkLocale).get(3).getName(),
                d.getAllPasswordsAsSortedList(dkLocale).get(3).getName().equals("HansPassword"));
        assertTrue("#5 element must be named 'mitPassword', and not: " + d.getAllPasswordsAsSortedList(dkLocale).get(4).getName(),
                d.getAllPasswordsAsSortedList(dkLocale).get(4).getName().equals("mitPassword"));
        assertTrue("#6 element must be named 'æblePassword', and not: " + d.getAllPasswordsAsSortedList(dkLocale).get(5).getName(),
                d.getAllPasswordsAsSortedList(dkLocale).get(5).getName().equals("æblePassword"));
        assertTrue("#7 element must be named 'øPassword', and not: " + d.getAllPasswordsAsSortedList(dkLocale).get(6).getName(),
                d.getAllPasswordsAsSortedList(dkLocale).get(6).getName().equals("øPassword"));
        assertTrue("#8 element must be named 'AAseassword', and not: " + d.getAllPasswordsAsSortedList(dkLocale).get(7).getName(),
                d.getAllPasswordsAsSortedList(dkLocale).get(7).getName().equals("AAseassword"));
        assertTrue("#9 element must be named 'åsePassword', and not: " + d.getAllPasswordsAsSortedList(dkLocale).get(8).getName(),
                d.getAllPasswordsAsSortedList(dkLocale).get(8).getName().equals("åsePassword"));
        assertTrue("#10 element must be named 'ÅsePassword', and not: " + d.getAllPasswordsAsSortedList(dkLocale).get(9).getName(),
                d.getAllPasswordsAsSortedList(dkLocale).get(9).getName().equals("ÅsePassword"));
        // check default locale value
        assertTrue("At 6. check Locale default value has change from " + origDefault.getLanguage() + " to " + Locale.getDefault().getLanguage(), Locale.getDefault().getLanguage() == origDefault.getLanguage());

        // Correct sequence in UK: "AAseassword", "abePassword", "æblePassword", "åsePassword", "ÅsePassword", "defaultpassword",
        //                         "ditPassword", "HansPassword", "mitPassword", "øPassword"
        assertTrue("#1 element must be named 'AAseassword', and not: " + d.getAllPasswordsAsSortedList(ukLocale).get(0).getName(),
                d.getAllPasswordsAsSortedList(ukLocale).get(0).getName().equals("AAseassword"));
        assertTrue("#2 element must be named 'abePassword', and not: " + d.getAllPasswordsAsSortedList(ukLocale).get(1).getName(),
                d.getAllPasswordsAsSortedList(ukLocale).get(1).getName().equals("abePassword"));
        assertTrue("#3 element must be named 'æblePassword', and not: " + d.getAllPasswordsAsSortedList(ukLocale).get(2).getName(),
                d.getAllPasswordsAsSortedList(ukLocale).get(2).getName().equals("æblePassword"));
        assertTrue("#4 element must be named 'åsePassword', and not: " + d.getAllPasswordsAsSortedList(ukLocale).get(3).getName(),
                d.getAllPasswordsAsSortedList(ukLocale).get(3).getName().equals("åsePassword"));
        assertTrue("#5 element must be named 'ÅsePassword', and not: " + d.getAllPasswordsAsSortedList(ukLocale).get(4).getName(),
                d.getAllPasswordsAsSortedList(ukLocale).get(4).getName().equals("ÅsePassword"));
        assertTrue("#6 element must be named 'defaultpassword', and not: " + d.getAllPasswordsAsSortedList(ukLocale).get(5).getName(),
                d.getAllPasswordsAsSortedList(ukLocale).get(5).getName().equals("defaultpassword"));
        assertTrue("#7 element must be named 'ditPassword', and not: " + d.getAllPasswordsAsSortedList(ukLocale).get(6).getName(),
                d.getAllPasswordsAsSortedList(ukLocale).get(6).getName().equals("ditPassword"));
        assertTrue("#8 element must be named 'HansPassword', and not: " + d.getAllPasswordsAsSortedList(ukLocale).get(7).getName(),
                d.getAllPasswordsAsSortedList(ukLocale).get(7).getName().equals("HansPassword"));
        assertTrue("#9 element must be named 'mitPassword', and not: " + d.getAllPasswordsAsSortedList(ukLocale).get(8).getName(),
                d.getAllPasswordsAsSortedList(ukLocale).get(8).getName().equals("mitPassword"));
        assertTrue("#10 element must be named 'øPassword', and not: " + d.getAllPasswordsAsSortedList(ukLocale).get(9).getName(),
                d.getAllPasswordsAsSortedList(ukLocale).get(9).getName().equals("øPassword"));
        // check default locale value
        assertTrue("At 7. check Locale default value has change from " + origDefault.getLanguage() + " to " + Locale.getDefault().getLanguage(), Locale.getDefault().getLanguage() == origDefault.getLanguage());
    }
    /** Make a clone of param config and let its name be nameOfClone.
     * @param config
     * @param nameOfClone
     * @return a clone of config */
    private DomainConfiguration cloneConfigWithNewName(DomainConfiguration config, String nameOfClone) {
        Iterator<SeedList> seedIterator = config.getSeedLists();
        Iterator<Password> passwordIterator = config.getPasswords();
        List<Password> passwordList = new ArrayList<Password>();
        List<SeedList> seedListList = new ArrayList<SeedList>();
        while (seedIterator.hasNext()) {
            seedListList.add(seedIterator.next());
        }
        while (passwordIterator.hasNext()) {
            passwordList.add(passwordIterator.next());
        }
        return new DomainConfiguration(nameOfClone, config.getDomain(), seedListList, passwordList);
    }

    private Password createDefaultPassword(String name) {
        return new Password(name, "no comments", "Domain is KB; SB", "Realm", "svc", "ThePassword");
    }
    /** Make a clone of the given password with a new name.
     * @param thePassword
     * @param nameOfClone
     * @return a clone of thePassword
     */
    private Password clonePasswordWithNewName(Password thePassword, String nameOfClone) {
        return new Password(nameOfClone, thePassword.getComments(), thePassword.getPasswordDomain(),
                thePassword.getRealm(),
                thePassword.getUsername(),
                thePassword.getPassword());
    }


}
