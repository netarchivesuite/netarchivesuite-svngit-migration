/* $Id$
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
package dk.netarkivet.systemtest.environment;

import dk.netarkivet.systemtest.TestLogger;
import dk.netarkivet.systemtest.page.PageHelper;
import org.jaccept.TestEventManager;

public class ApplicationManager {
    protected final TestLogger log = new TestLogger(getClass());
    private final TestEnvironmentManager environmentManager;

    public ApplicationManager(TestEnvironmentManager environmentManager) {
        this.environmentManager = environmentManager;
    }

    public void restartGUI() {
        try {
            log.info("Restarting GUI");
            environmentManager.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, "./conf/kill_GUIApplication.sh");
            environmentManager.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, "./conf/start_GUIApplication.sh");
            waitForGUIToStart(10);
        } catch (Exception e) {
            throw new RuntimeException("Failed to redeploy GUI", e);
        }
    }
    public void redeployGUI() {
        try {
            log.info("Redeploying GUI");
            environmentManager.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, "rm -r tmpdircommon/*");
            environmentManager.runCommandWithoutQuotes("prepare_test_db.sh");
            environmentManager.runCommandWithoutQuotes("scp -r release_software_dist/$TESTX/lib/* " +
                    TestEnvironment.JOB_ADMIN_SERVER + ":~/$TESTX/lib" );
            environmentManager.runCommandWithoutQuotes("scp -r release_software_dist/$TESTX/webpages/* " +
                    TestEnvironment.JOB_ADMIN_SERVER + ":~/$TESTX/webpages" );
            environmentManager.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, "./conf/kill_GUIApplication.sh");
            environmentManager.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, "./conf/start_GUIApplication.sh");
        } catch (Exception e) {
            throw new RuntimeException("Failed to redeploy GUI", e);
        }
    }

    public void waitForGUIToStart(int maxNumberOfSecondsToWait) {
        int numberOfSecondsToWaiting = 0;
        TestEventManager.getInstance().addStimuli("Waiting for GUI to start.");
        while (numberOfSecondsToWaiting++ < maxNumberOfSecondsToWait) {
            PageHelper.reloadSubPage("HarvestDefinition");
            if (PageHelper.getWebDriver().getPageSource().contains("Definitions")) {
                System.out.println();
                return;
            } else {
                System.out.print(".");
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
            }
        }
    }
}