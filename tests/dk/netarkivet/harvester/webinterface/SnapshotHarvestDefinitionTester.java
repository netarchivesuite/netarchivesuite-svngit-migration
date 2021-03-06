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

import javax.servlet.jsp.PageContext;

import dk.netarkivet.common.exceptions.ForwardedToErrorPage;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.harvester.datamodel.FullHarvest;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;
import dk.netarkivet.testutils.StringAsserts;

/** Unit-test for the SnapshotHarvestDefinition class. */
public class SnapshotHarvestDefinitionTester extends HarvesterWebinterfaceTestCase {
    public SnapshotHarvestDefinitionTester(String s) {
        super(s);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Tests the SnapshotHarvestDefinition.processRequest() method.
     *
     * @throws Exception
     */
    public void testProcessRequest() throws Exception {
        MockupServletRequest request = new MockupServletRequest();
        // Should just return with update==null
        HarvestDefinitionDAO dao = HarvestDefinitionDAO.getInstance();
        String newHDname = "fnord";
        assertNull("Should not have fnord before creation",
                   dao.getHarvestDefinition(newHDname));
        I18n I18N = new I18n(
                dk.netarkivet.common.Constants.TRANSLATIONS_BUNDLE);
        PageContext pageContext = new HarvesterWebinterfaceTestCase.TestPageContext(
                request);
        SnapshotHarvestDefinition.processRequest(pageContext, I18N);

        request.addParameter(Constants.UPDATE_PARAM, "yes");
        request.addParameter(Constants.DOMAIN_PARAM, newHDname);

        assertNull("Should not have fnord before creation",
                   dao.getHarvestDefinition(newHDname));
        request.addParameter(Constants.CREATENEW_PARAM, "yes");
        request.addParameter(Constants.DOMAIN_OBJECTLIMIT_PARAM, "-1");
        request.addParameter(Constants.DOMAIN_BYTELIMIT_PARAM, "117");
        request.addParameter(Constants.HARVEST_PARAM, newHDname);
        request.addParameter(Constants.COMMENTS_PARAM, "You did not see this");
        assertCallChecksArgument(request, newHDname,
                                 Constants.HARVEST_PARAM);
        SnapshotHarvestDefinition.processRequest(pageContext, I18N);

        FullHarvest newHD = (FullHarvest) dao.getHarvestDefinition(newHDname);
        assertNotNull("Should have fnord after creation", newHD);
        assertEquals("Should have right name", newHDname, newHD.getName());
        assertEquals("Should have right bytelimit", 117, newHD.getMaxBytes());
        assertEquals("Should have right comments", "You did not see this",
                     newHD.getComments());
        assertNull("Old harvest id should be null",
                   newHD.getPreviousHarvestDefinition());
        assertFalse("Should be inactive", newHD.getActive());

        assertCallIsForbidden(request, newHDname, "already exists");
    }

    private void assertCallChecksArgument(MockupServletRequest request,
                                          String newHDName,
                                          String toFind) {
        String removedParameter = request.getParameter(toFind);
        try {
            I18n I18N = new I18n(
                    dk.netarkivet.common.Constants.TRANSLATIONS_BUNDLE);
            request.removeParameter(toFind);
            PageContext pageContext = new HarvesterWebinterfaceTestCase.TestPageContext(
                    request);
            SnapshotHarvestDefinition.processRequest(pageContext, I18N);
            fail("Should complain about missing " + toFind);
        } catch (ForwardedToErrorPage e) {
            StringAsserts.assertStringContains(
                    "Should mention " + toFind + " in msg",
                    toFind, e.getMessage());
        }
        request.addParameter(toFind, removedParameter);
        HarvestDefinitionDAO dao = HarvestDefinitionDAO.getInstance();
        assertNull("Should not have fnord before creation",
                   dao.getHarvestDefinition(newHDName));
    }

    private void assertCallIsForbidden(MockupServletRequest request,
                                       String newHDName,
                                       String toFind) {
        try {
            I18n I18N = new I18n(
                    dk.netarkivet.common.Constants.TRANSLATIONS_BUNDLE);
            PageContext pageContext = new HarvesterWebinterfaceTestCase.TestPageContext(
                    request);
            SnapshotHarvestDefinition.processRequest(pageContext, I18N);
            fail("Should complain about missing " + toFind);
        } catch (ForwardedToErrorPage e) {
            StringAsserts.assertStringContains(
                    "Should mention " + toFind + " in msg",
                    toFind, e.getMessage());
        }
        HarvestDefinitionDAO dao = HarvestDefinitionDAO.getInstance();
        assertNotNull("Should have fnord after creation",
                      dao.getHarvestDefinition(newHDName));
    }

}