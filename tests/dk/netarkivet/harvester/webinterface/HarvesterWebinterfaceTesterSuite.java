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

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * A suite of harvester webinterface tests.
 */
public class HarvesterWebinterfaceTesterSuite {
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(HarvesterWebinterfaceTesterSuite.class.getName());
        addToSuite(suite);
        return suite;
    }

    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(DomainDefinitionTester.class);
        //suite.addTestSuite(EventHarvestTester.class); Fails in Hudson
        // Not quite working with JSP compilation yet
        //suite.addTestSuite(HarveststatusPerdomainTester.class);
        suite.addTestSuite(HistorySiteSectionTester.class);
        suite.addTestSuite(ScheduleDefinitionTester.class);
        suite.addTestSuite(SelectiveHarvestUtilTester.class);
        suite.addTestSuite(SnapshotHarvestDefinitionTester.class);
        suite.addTestSuite(HarvestStatusTester.class);
    }

    public static void main(String args[]) {
        String args2[] = {"-noloading", HarvesterWebinterfaceTesterSuite.class.getName()};

        TestRunner.main(args2);
    }
}
