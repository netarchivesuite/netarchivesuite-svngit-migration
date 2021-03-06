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

package dk.netarkivet.common;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import dk.netarkivet.common.distribute.CommonDistributeTesterSuite;
import dk.netarkivet.common.distribute.indexserver.CommonDistributeIndexserverTesterSuite;
import dk.netarkivet.common.exceptions.CommonExceptionsTesterSuite;
import dk.netarkivet.common.lifecycle.CommonLifecycleTesterSuite;
import dk.netarkivet.common.management.CommonManagementTesterSuite;
import dk.netarkivet.common.tools.CommonToolsTesterSuite;
import dk.netarkivet.common.utils.CommonUtilsTesterSuite;
import dk.netarkivet.common.utils.arc.CommonUtilsArcTesterSuite;
import dk.netarkivet.common.utils.batch.CommonUtilsBatchTesterSuite;
import dk.netarkivet.common.utils.cdx.CommonUtilsCdxTesterSuite;
import dk.netarkivet.common.utils.warc.CommonUtilsWarcTesterSuite;
import dk.netarkivet.common.webinterface.CommonWebinterfaceTesterSuite;

/**
 * This class runs all the common module unit tests.
 */
public class UnitTesterSuite {
    public static void addToSuite(TestSuite suite) {
        CommonUtilsArcTesterSuite.addToSuite(suite);
        CommonUtilsWarcTesterSuite.addToSuite(suite);
        CommonUtilsBatchTesterSuite.addToSuite(suite);
        CommonUtilsCdxTesterSuite.addToSuite(suite);
        CommonsTesterSuite.addToSuite(suite);
        CommonLifecycleTesterSuite.addToSuite(suite);
        CommonUtilsTesterSuite.addToSuite(suite);
        CommonDistributeIndexserverTesterSuite.addToSuite(suite);
        CommonDistributeTesterSuite.addToSuite(suite);
        CommonExceptionsTesterSuite.addToSuite(suite);
        CommonManagementTesterSuite.addToSuite(suite);
        CommonToolsTesterSuite.addToSuite(suite);
        CommonWebinterfaceTesterSuite.addToSuite(suite);
    }

    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(UnitTesterSuite.class.getName());

        addToSuite(suite);

        return suite;
    }

    public static void main(String[] args) {
        String[] args2 = {"-noloading", UnitTesterSuite.class.getName()};
        TestRunner.main(args2);
    }
}
