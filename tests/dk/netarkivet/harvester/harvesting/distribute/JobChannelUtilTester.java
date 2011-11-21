/*$Id: JobChannelUtilTester.java 1523 2010-08-09 08:42:50Z mikis $
* $Revision: 1523 $
* $Date: 2010-08-09 10:42:50 +0200 (Mon, 09 Aug 2010) $
* $Author: mikis $
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2011 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.ChannelsTester;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.JobPriority;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/** 
 * Validates the <code>JobChannelUtil</code> class funtionality. 
 */
public class JobChannelUtilTester extends ChannelsTester {

    ReloadSettings rs = new ReloadSettings();
    public JobChannelUtilTester(String s) {
        super(s);
    }
    /**
     * This tests the getChannel method for generating the right priorities
     * in both the version taking the priority as argument and reading the
     * priority from settings.
     */
    public void testGetAnyHaco() {
        String env = Settings.get(CommonSettings.ENVIRONMENT_NAME);

        // Test setting low priority in settings
        Settings.set(HarvesterSettings.HARVEST_CONTROLLER_PRIORITY,
                     "LOWPRIORITY");
        resetChannels();
        JobPriority priority = JobPriority.valueOf(
                Settings.get(HarvesterSettings.HARVEST_CONTROLLER_PRIORITY));
        
        assertEquals("Channel should be low priority",
                     env + "_COMMON_ANY_LOWPRIORITY_HACO",
                     JobChannelUtil.getChannel(priority).getName());


        // Test setting high priority in settings
        Settings.set(HarvesterSettings.HARVEST_CONTROLLER_PRIORITY,
                     "HIGHPRIORITY");
        resetChannels();
        priority = JobPriority.valueOf(
                Settings.get(HarvesterSettings.HARVEST_CONTROLLER_PRIORITY));
        assertEquals("Channel should be high priority",
                     env + "_COMMON_ANY_HIGHPRIORITY_HACO",
                     JobChannelUtil.getChannel(priority).getName());
    }
}