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
package dk.netarkivet.harvester.harvesting.frontier;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Test suite for the classes
 * in package dk.netarkivet.harvester.harvesting.
 */
public class HarvesterHarvestingFrontierTesterSuite {
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(HarvesterHarvestingFrontierTesterSuite.class.getName());

        addToSuite(suite);

        return suite;
    }
    
    /**
     * Add tests to suite.
     * One line for each unit-test class in this testsuite.
     * @param suite the suite.
     */
    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(FullFrontierReportTest.class);
        suite.addTestSuite(FrontierReportFilterTest.class);
        suite.addTestSuite(FrontierReportLineTest.class);
        suite.addTestSuite(InMemoryFrontierReportTest.class);
    }

    public static void main(String[] args) {
        String[] args2 = {"-noloading", HarvesterHarvestingFrontierTesterSuite.class.getName()};

        TestRunner.main(args2);
        //junit.swingui.TestRunner.main(args2);
        try {
            Thread.sleep(100000000);
        } catch (InterruptedException e) {
            //just testing, remove block
        }
    }
}
