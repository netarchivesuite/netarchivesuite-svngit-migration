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
package dk.netarkivet.harvester.scheduler;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.RememberNotifications;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.XmlUtils;
import dk.netarkivet.harvester.datamodel.*;
import dk.netarkivet.testutils.TestFileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *  This will add the components to an empty NetarchiveSuite database
 *  that is necessary for the two unit tests in the SchedulerTesterSuite.
 *  
 *  Add domain netarkivet.dk with two seedlist
 *  Add the three schedules Dagligt, Once_a_day, OnceOnly
 *  Add one selective harvestdefinition (isActive=true)
 */
public class SchedulerDatabaseBuilder {
    
    public static void main(String[] args) throws Exception {
        SchedulerDatabaseBuilder sdb = new SchedulerDatabaseBuilder();
        sdb.doWork();
        sdb.close();
    }

    public SchedulerDatabaseBuilder() throws Exception {
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        TestInfo.WORKING_DIR.mkdirs();
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR,
                TestInfo.WORKING_DIR);

        Settings.set(CommonSettings.DB_BASE_URL, "jdbc:derby:"
                + TestInfo.WORKING_DIR.getCanonicalPath() + "/fullhddb");
        
        DatabaseTestUtils.getHDDB(new File(TestInfo.BASEDIR, "emptyhddb.jar"),
                "fullhddb",
                TestInfo.WORKING_DIR);
        HarvestDAOUtils.resetDAOs();
        Settings.set(CommonSettings.NOTIFICATIONS_CLASS,
                RememberNotifications.class.getName());
    }

    private void close() throws Exception {
        DatabaseTestUtils.dropHDDB();
        File destinationDir = new File(TestInfo.ORIGINALS_DIR, "fullhddb-"
                + System.currentTimeMillis());
        destinationDir.mkdir();
        System.out.println("Copied database to " 
                + destinationDir.getAbsolutePath());
        TestFileUtils.copyDirectoryNonCVS(new File(TestInfo.WORKING_DIR,
                "fullhddb"), destinationDir);
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        
        
        // Zip jarfile as schedulerDB.jar and copy to TestInfo.ORIGINALS_DIR
        
        //zipDirectory(new File(TestInfo.WORKING_DIR, "fullhddb"), 
        //        new File(TestInfo.ORIGINALS_DIR, "schedulerDB.jar"));        
        
    }

    private void doWork() {
        
        // Add templates.
        TemplateDAO tdao = TemplateDAO.getInstance();
        
        HeritrixTemplate ht;
        
        File DefaultOrderXml = new File(TestInfo.orderTemplatesOriginalsDir,
            "default_orderxml.xml");
        File OneLevelOrderXml = new File(TestInfo.orderTemplatesOriginalsDir,
                "OneLevel-order.xml");
        File FullSiteOrderXml = new File(TestInfo.orderTemplatesOriginalsDir,
            "FullSite-order.xml");
        File Max_20_2_OrderXml = new File(TestInfo.orderTemplatesOriginalsDir,
            "Max_20_2-order.xml");
        
       
        ht = new HeritrixTemplate(XmlUtils.getXmlDoc(DefaultOrderXml), true);
        tdao.create("default_orderxml", ht);
        
        ht = new HeritrixTemplate(XmlUtils.getXmlDoc(FullSiteOrderXml), true);
        tdao.create("FullSite-order", ht);
        
        ht = new HeritrixTemplate(XmlUtils.getXmlDoc(OneLevelOrderXml), true);
        tdao.create("OneLevel-order", ht);
        
        ht = new HeritrixTemplate(XmlUtils.getXmlDoc(Max_20_2_OrderXml), true);
        tdao.create("Max_20_2-order", ht);
        
        // Create domain netarkivet.dk and its configurations.
        DomainDAO dao = DomainDAO.getInstance();
        Domain d = Domain.getDefaultDomain("netarkivet.dk");
        SeedList englishSeed = new SeedList("english", 
            "http://www.netarkivet.dk/index-en.htm/n");
        SeedList danishSeed = new SeedList("danish", 
            "http://www.netarkivet.dk/website/links/index-da.htm/n");

        d.addSeedList(englishSeed);
        d.addSeedList(danishSeed);

        DomainConfiguration cfg1 = new DomainConfiguration(
                "Dansk_netarkiv_fuld_dybde", d, 
                Arrays.asList(danishSeed), new ArrayList<Password>());
        cfg1.setMaxBytes(
                dk.netarkivet.harvester.datamodel.Constants.DEFAULT_MAX_BYTES);
        cfg1.setOrderXmlName("FullSite-order");
        d.addConfiguration(cfg1);
        
        DomainConfiguration cfg2 = new DomainConfiguration(
                "Engelsk_netarkiv_et_niveau", d, 
                Arrays.asList(englishSeed), new ArrayList<Password>());
        cfg2.setMaxBytes(
                dk.netarkivet.harvester.datamodel.Constants.DEFAULT_MAX_BYTES);
        cfg2.setOrderXmlName("OneLevel-order");
        d.addConfiguration(cfg2);
        dao.create(d);
        
        // Create schedules:
        
        ScheduleDAO sDao = ScheduleDAO.getInstance();        
        Frequency f = Frequency.getNewInstance(TimeUnit.DAILY.ordinal(),
                true, 1, null, null, null, null);
        
        Schedule s1 = Schedule.getInstance(null, null, f,
                "Dagligt", "Dagligt, nårsomhelt på dagen");
        
        Schedule s2 = Schedule.getInstance(null, null, f, 
                "Once_a_day", "Once a day, anytime");
        Schedule s3 = Schedule.getInstance(null, 1, f, 
                "OnceOnly", "En gang, med det samme");
        sDao.create(s1);
        sDao.create(s2);
        sDao.create(s3);
        
        // Create a selective harvestdefinition and save it in our database.
        
        List<DomainConfiguration> dcs = new ArrayList<DomainConfiguration>();
        dcs.add(cfg2);
        
        PartialHarvest ph =  
            PartialHarvest.createPartialHarvest(dcs, s2,
                    "Testhøstning", "No Comments", "Everybody");
        ph.setActive(true);
        
        HarvestDefinitionDAO hdd = HarvestDefinitionDAO.getInstance();
        hdd.create(ph);
    }
}
