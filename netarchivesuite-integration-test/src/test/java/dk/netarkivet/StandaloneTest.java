/* File:            $Id$
 * Revision:        $Revision$
 * Author:          $Author$
 * Date:            $Date$
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
package dk.netarkivet;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeTest;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

/**
 * Stand-alone test requires a custom setup of the test system, and can
 * therefore not be run together with other tests after the test system has been
 * deploy.
 */
public abstract class StandaloneTest extends SystemTest {

    @BeforeTest (alwaysRun = true) 
    public void startTestSystem() throws Exception {
        if (System.getProperty("systemtest.deploy", "false").equals("true")) {
            runCommandWithEnvironment(getStartupScript());
        }
        int numberOfSecondsToWaiting = 0;
        int maxNumberOfSecondsToWait = 60;
        System.out.print("Waiting for GUI to start");
        while (numberOfSecondsToWaiting++ < maxNumberOfSecondsToWait) {
            driver.get(baseUrl + "/HarvestDefinition/");
            if (selenium.isTextPresent("Definitions")) {
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
        throw new RuntimeException("Failed to load GUI");
    }

    /**
     * Defines the test system startup script to run. May be overridden by 
     * concrete classes.
     * @return The startup script to run
     */
    public String getStartupScript() {
        return "all_test_db.sh";
    }

    /**
     * Identifies the test on the test system. More concrete this value will be
     * used for the test environment variable.
     * 
     * @return
     */
    public String getTestX() {
        return "SystemTest";
    }

    /**
     * Uses ssh to run the indicated command on the test deployment server (kb-prod-udv-001.kb.dk). The system test 
     * environment variables: 
     * <ul>
     * <li>TIMESTAMP = svn revision
     * <li>PORT = systemtest.port property or 8071 if undefined
     * <li>MAILRECEIVERS = systemtest.mailrecievers property
     * <li>TESTX = SystemTest
     * </ul>
     * are set prior to running the command.
     * @param remoteCommand The command to run on the test server
     * @throws Exception It apparently didn't work.
     */
    protected void runCommandWithEnvironment(String remoteCommand)
    throws Exception {
        BufferedReader inReader = null;
        BufferedReader errReader = null;
        JSch jsch = new JSch();

        Session session = jsch.getSession("test", "kb-prod-udv-001.kb.dk");
        // session.setPassword("test123");
        session.setTimeout(1000);

        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);

        session.connect();

        String version = lookupRevisionValue();
        String setTimeStampCommand = "export TIMESTAMP=" + version;
        String setPortCommand = "export PORT=" + getPort();
        String setMailReceiversCommand = "export MAILRECEIVERS="
            + System.getProperty("systemtest.mailrecievers");
        String setTestCommand = "export TESTX=" + getTestX();
        String setPathCommand = "source /etc/bashrc ; source /etc/profile; source ~/.bash_profile";

        String command = setPathCommand + ";" + setTimeStampCommand + ";"
        + setPortCommand + ";" + setMailReceiversCommand + ";"
        + setTestCommand + ";" + remoteCommand;

        long startTime = System.currentTimeMillis();
        log.info("Running JSch command: " + command);

        Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand(command);
        channel.setInputStream(null);
        ((ChannelExec) channel).setErrStream(null);

        InputStream in = channel.getInputStream();
        InputStream err = ((ChannelExec) channel).getErrStream();
        
        channel.connect(1000);
        log.debug("Channel connected");

        inReader = new BufferedReader(new InputStreamReader(in));
        
        int numberOfSecondsWaiting = 0;
        int maxNumberOfSecondsToWait = 60*10;
        while (true) {
            if (channel.isClosed()) {
                log.info("Command finished in "
                        + (System.currentTimeMillis() - startTime) / 1000
                        + " seconds. " + "Exit code was "
                        + channel.getExitStatus());
                break;
            } else if ( numberOfSecondsWaiting > maxNumberOfSecondsToWait) {
                log.info("Command not finished after " + maxNumberOfSecondsToWait + " seconds. " +
                		"Forcing disconnect.");
                channel.disconnect();
                break;
            }
            try {
                Thread.sleep(1000);

                String s;
                while ((s = inReader.readLine()) != null) {
                    System.out.println("ssh: " + s);
                }
            } catch (InterruptedException ie) {
            }
        }

        errReader = new BufferedReader(new InputStreamReader(err));

        String s;
        StringBuffer sb = new StringBuffer();
        while ((s = errReader.readLine()) != null) {
            sb.append(s).append("\n");
        }
        log.debug(sb);
        
        String errors = sb.toString();
        log.info("Finished command");
        if (errors.contains("ERROR") || errors.contains("Exception")) {
            throw new RuntimeException(
                    "Console output from deployment of NetarchiveSuite to the test system "
                    + "indicated a error: " + errors);
        }
    }

    /**
     * The deployment script on the test server expects the 'TIMESTAMP' variable
     * to be set to the value between the 'NetarchiveSuite-' and '.zip' part of
     * the NetarchiveSuite zip file in the 'target/deploy' directory.
     * 
     * @return
     */
    private String lookupRevisionValue() {
        String revisionValue = null;
        if (System.getProperty("systemtest.version") != null) {
            revisionValue = System.getProperty("systemtest.version");
        } else { 
            File dir = new File("deploy");
            String[] children = dir.list();
            int testXValueStart = "NetarchiveSuite-".length();
            for (String fileName : children) {
                int zipPrefixPos = fileName.indexOf(".zip");
                if (fileName.contains("NetarchiveSuite-")
                        && zipPrefixPos > testXValueStart) {
                    revisionValue = fileName.substring(testXValueStart,
                            zipPrefixPos);
                }
            }
        }
        return revisionValue;
    }

    @AfterMethod
    /**
     * Takes care of failure situations. This includes: <ol>
     * <li> Generate a a screen dump of the page failing the test.
     * <ol>
     * 
     * This method is called by TestNG.
     * 
     * @param result The result which TestNG will inject
     */
    public void onFailure(ITestResult result) { 
        if (!result.isSuccess()) { 
            log.info("Test failure, dumping screenshot as " + "target/failurescreendumps/" + 
                    result.getMethod() + ".png");
            File scrFile = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            try {
                FileUtils.copyFile(scrFile, new File("failurescreendumps/" + result.getMethod() + ".png"));
            } catch (IOException e) {
                log.error("Failed to save screendump on error");
            }
        }
    }
}
