/*$Id$
* $Revision$
* $Date$
* $Author$
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
package dk.netarkivet.harvester.webinterface;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.webinterface.GUIWebServer;
import dk.netarkivet.harvester.datamodel.DataModelTestCase;
import dk.netarkivet.harvester.datamodel.TestInfo;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * Unit-test for the GUIWebServer class when the DefinitionsSiteSection
 * is loaded.
 * FIXME Some of these tests can be merged into the GUIWebServerTester.
 */
public class HarvestDefinitionGUITester extends DataModelTestCase {
    private GUIWebServer gui;

    public HarvestDefinitionGUITester(String s) {
        super(s);
    }
    ReloadSettings rs = new ReloadSettings();

    public void setUp() throws Exception {
        rs.setUp();
        super.setUp();
        // Add a DefinitionsSiteSection to the list of Sitesections being loaded
        // when GUIWebServer starts.
        Settings.set(
                CommonSettings.SITESECTION_WEBAPPLICATION,
                TestInfo.HARVESTDEFINITION_JSP_DIR);
        Settings.set(
                CommonSettings.SITESECTION_CLASS,
                TestInfo.HARVESTDEFINITION_SITESECTIONCLASS);

        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
    }

    public void tearDown() throws Exception {
        super.tearDown();
        if (gui != null) {
            gui.cleanup();
        }
        rs.tearDown();
    }

    public void testSettingsWebappFault() {
        try {
            Settings.set(CommonSettings.SITESECTION_WEBAPPLICATION,
                         "not_a_webapp");
            gui = GUIWebServer.getInstance();
            fail("Should throw an error on invalid webapp");
        } catch (IOFailure e) {
            //expected
        }
    }

    public void testSettingsPortFault() {
        try {
            Settings.set(CommonSettings.HTTP_PORT_NUMBER, "not_a_number");
            gui = GUIWebServer.getInstance();
            fail("Should throw an error on invalid port number");
        } catch (NumberFormatException e) {
            //expected
        }
    }

    public void testSettingsPortWrong() {
        Settings.set(CommonSettings.HTTP_PORT_NUMBER, "42");
        try {
            gui = GUIWebServer.getInstance();
            fail("Should throw an error on invalid port number");
        } catch (IOFailure e) {
            //expected
        }
    }

    /**
     * Test, that GUIWebServer.getInstance throws UnknownID exception
     * if templates tables does not contain the template with name
     * Settings.DOMAIN_DEFAULT_ORDERXML
     * This tests the partial solution of bug 916
     * @throws InterruptedException
     */
     public void testExitWithoutDefaultTemplateInTemplatesTable() throws InterruptedException {
         String[] webApps = Settings.getAll(
                 CommonSettings.SITESECTION_WEBAPPLICATION);
         boolean harvestdefinitionFound = false;
         for (String webapp : webApps) {
             if (webapp.contains("HarvestDefinition")) {
                 harvestdefinitionFound = true;
                 break;
             }
         }
         assertTrue("Test-requirement not met: "
                 + "DefinitionsSiteSection not in default settings",
                 harvestdefinitionFound);
     }

}
