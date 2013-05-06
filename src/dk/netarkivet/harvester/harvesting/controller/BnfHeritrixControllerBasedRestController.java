/* File:       $Id$
 * Revision:   $Revision$
 * Author:     $Author$
 * Date:       $Date$
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
package dk.netarkivet.harvester.harvesting.controller;

import dk.netarkivet.common.exceptions.*;
import dk.netarkivet.common.utils.*;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.harvesting.HeritrixFiles;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage.CrawlServiceInfo;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage.CrawlServiceJobInfo;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage.CrawlStatus;
import dk.netarkivet.harvester.harvesting.frontier.FullFrontierReport;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.crawler.framework.CrawlController;
//import org.archive.util.JmxUtils;

import javax.management.*;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import javax.management.remote.JMXConnector;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * This implementation of the HeritrixController interface starts Heritrix as a
 * separate process and uses JMX to communicate with it. Each instance executes
 * exactly one process that runs exactly one crawl job.
 * FIXME this code is in the process of conversion to REST, as Heritrix no longer responds to JMX
 */
public class BnfHeritrixControllerBasedRestController extends AbstractRESTHeritrixController {

    /** The logger for this class. */
    private static final Log log = LogFactory
            .getLog(BnfHeritrixControllerBasedRestController.class);

    /**
     * The below commands and attributes are copied from the attributes and
     * operations exhibited by the Heritrix MBeans of type CrawlJob and
     * CrawlService.Job, as they appear in JConsole.
     *
     * Only operations and attributes used in NAS are listed.
     */
    private static enum CrawlServiceAttribute {
        /** The number of alerts raised by Heritrix. */
        AlertCount,
        /** True if Heritrix is currently crawling, false otherwise. */
        IsCrawling,
        /** The ID of the job being currently crawled by Heritrix. */
        CurrentJob;

        /**
         * Returns the {@link CrawlServiceAttribute} enum value matching the
         * given name. Throws {@link UnknownID} if no match is found.
         *
         * @param name
         *            the attribute name
         * @return the corresponding {@link CrawlServiceAttribute} enum value.
         */
        public static CrawlServiceAttribute fromString(String name) {
            for (CrawlServiceAttribute att : values()) {
                if (att.name().equals(name)) {
                    return att;
                }
            }
            throw new UnknownID(name + " : unknown CrawlServiceAttribute !");
        }
    }
    
    /**
     * 
     * Enum listing the different job attributes available.
     *
     */
    private static enum CrawlServiceJobAttribute {
        /** The time in seconds elapsed since the crawl began. */
        CrawlTime,
        /** The current download rate in URI/s. */
        CurrentDocRate,
        /** The current download rate in kB/s. */
        CurrentKbRate,
        /** The number of URIs discovered by Heritrix. */
        DiscoveredCount,
        /** The average download rate in URI/s. */
        DocRate,
        /** The number of URIs downloaded by Heritrix. */
        DownloadedCount,
        /** A string summarizing the Heritrix frontier. */
        FrontierShortReport,
        /** The average download rate in kB/s. */
        KbRate,
        /** The job status (Heritrix status). */
        Status,
        /** The number of active toe threads. */
        ThreadCount;

        /**
         * Returns the {@link CrawlServiceJobAttribute} enum value matching the
         * given name. Throws {@link UnknownID} if no match is found.
         *
         * @param name
         *            the attribute name
         * @return the corresponding {@link CrawlServiceJobAttribute} enum
         *         value.
         */
        public static CrawlServiceJobAttribute fromString(String name) {
            for (CrawlServiceJobAttribute att : values()) {
                if (att.name().equals(name)) {
                    return att;
                }
            }
            throw new UnknownID(name + " : unknown CrawlServiceJobAttribute !");
        }
    }
    /**
     * Enum class defining the general operations available to 
     * the Heritrix operator.
     */
    private static enum CrawlServiceOperation {
        /** Adds a new job to an Heritrix instance. */
        addJob,
        /** Fetches the identifiers of pending jobs. */
        pendingJobs,
        /** Fetches the identifiers of completed jobs. */
        completedJobs,
        /** Shuts down an Heritrix instance. */
        shutdown,
        /** Instructs an Heritrix instance to starts crawling jobs. */
        startCrawling,
        /** Instructs an Heritrix instance to terminate the current job. */
        terminateCurrentJob;
    }
    
    /**
     * Enum class defining the Job-operations available to 
     * the Heritrix operator.
     */
    private static enum CrawlServiceJobOperation {
        /** Fetches the progress statistics string from an Heritrix instance. */
        progressStatistics,
        /**
         * Fetches the progress statistics legend string from an Heritrix
         * instance.
         */
        progressStatisticsLegend,
        /** Fetches the frontier report. */
        frontierReport;
    }
    
    /**
     * Shall we abort, if we lose the connection to Heritrix.
     */
    private static final boolean ABORT_IF_CONN_LOST = Settings
            .getBoolean(HarvesterSettings.ABORT_IF_CONNECTION_LOST);

    /**
     * The part of the Job MBean name that designates the unique id. For some
     * reason, this is not included in the normal Heritrix definitions in
     * JmxUtils, otherwise we wouldn't have to define it. I have committed a
     * feature request: http://webteam.archive.org/jira/browse/HER-1618
     */
    private static final String UID_PROPERTY = "uid";

    /**
     * The name that Heritrix gives to the job we ask it to create. This is part
     * of the name of the MBean for that job, but we can only retrieve the name
     * after the MBean has been created.
     */
    private String jobName;

    /** The header line (legend) for the statistics report. */
    private String progressStatisticsLegend;

    /**
     * The connector to the Heritrix MBeanServer.
     */
    private JMXConnector jmxConnector;

    /**
     * Max tries for a JMX operation.
     */
    private final int jmxMaxTries = JMXUtils.getMaxTries();

    /**
     * The name of the MBean for the submitted job.
     */
    private String crawlServiceJobBeanName;

    /**
     * The name of the main Heritrix MBean.
     */
    private String crawlServiceBeanName;

    /**
     * Create a BnfHeritrixController object.
     *
     * @param files
     *            Files that are used to set up Heritrix.
     */
    public BnfHeritrixControllerBasedRestController(HeritrixFiles files) {
        super(files);
    }

    /**
     * Initialize the JMXconnection to the Heritrix.
     * @throws IOFailure
     *             If Heritrix dies before initialization, or we encounter any
     *             problems during the initialization.
     * @see HeritrixController#initialize()
     */
    @Override
    public void initialize() {
        if (processHasExited()) {
            String errMsg = "Heritrix process of " + this
                    + " died before initialization";
            log.warn(errMsg);
            throw new IOFailure(errMsg);
        }
        
        log.info("Abort, if we lose the connection "
                + "to Heritrix, is " + ABORT_IF_CONN_LOST);  
        //initJMXConnection();
        
        //log.info("JMX connection initialized successfully");

        log.info("Adding harvest job to Heritrix by REST");
        // FIXME add code to add job to Heritrix by REST using 
        // Step 1: Verify that other jobs are not already pending in the Heritrix instance
        // Step 2: Add harvest job
        
        
        log.info("Harvest job added to Heritrix by REST");
        
//// 	Note this code does not work any more, as Heritrix no longer responds to JMX
////        crawlServiceBeanName = "org.archive.crawler:" + JmxUtils.NAME
////                + "=Heritrix," + JmxUtils.TYPE + "=CrawlService,"
////                + JmxUtils.JMX_PORT + "=" + getJmxPort() + ","
////                + JmxUtils.GUI_PORT + "=" + getGuiPort() + "," + JmxUtils.HOST
////                + "=" + getHostName();
//
//        // We want to be sure there are no jobs when starting, in case we got
//        // an old Heritrix or somebody added jobs behind our back.
//        TabularData doneJobs = (TabularData) executeMBeanOperation(
//                CrawlServiceOperation.completedJobs);
//        TabularData pendingJobs = (TabularData) executeMBeanOperation(
//                CrawlServiceOperation.pendingJobs);
//        if (doneJobs != null && doneJobs.size() > 0 || pendingJobs != null
//                && pendingJobs.size() > 0) {
//            throw new IllegalState(
//                    "This Heritrix instance is in a illegalState! "
//                            + "This instance has either old done jobs ("
//                            + doneJobs + "), or old pending jobs ("
//                            + pendingJobs + ").");
//        }
//        // From here on, we can assume there's only the one job we make.
//        // We'll use the arc file prefix to name the job, since the prefix
//        // already contains the harvest id and job id.
//        HeritrixFiles files = getHeritrixFiles();
//        executeMBeanOperation(CrawlServiceOperation.addJob, files
//                .getOrderXmlFile().getAbsolutePath(), files.getArchiveFilePrefix(),
//                getJobDescription(), files.getSeedsTxtFile().getAbsolutePath());
//
//        jobName = getJobName();
//// FIXME this code does not work any more, as Heritrix no longer responds to JMX
////        crawlServiceJobBeanName = "org.archive.crawler:" + JmxUtils.NAME + "="
////                + jobName + "," + JmxUtils.TYPE + "=CrawlService.Job,"
////                + JmxUtils.JMX_PORT + "=" + getJmxPort() + ","
////                + JmxUtils.MOTHER + "=Heritrix," + JmxUtils.HOST + "="
////                + getHostName();
    }

    @Override
    public void requestCrawlStart() {
        executeMBeanOperation(CrawlServiceOperation.startCrawling);
    }

    @Override
    public void requestCrawlStop(String reason) {
        executeMBeanOperation(CrawlServiceOperation.terminateCurrentJob);
    }

    /**
     * Return the URL for monitoring this instance.
     *
     * @return the URL for monitoring this instance.
     */
    public String getHeritrixConsoleURL() {
        return "http://" + SystemUtils.getLocalHostName() + ":" + getGuiPort();
    }

    /**
     * Cleanup after an Heritrix process. This entails sending the shutdown
     * command to the Heritrix process, and killing it forcefully, if it is
     * still alive after waiting the period of time specified by the
     * CommonSettings.PROCESS_TIMEOUT setting.
     * @param crawlDir the crawldir to cleanup
     * @see HeritrixController#cleanup()
     */
    public void cleanup(File crawlDir) {
        // Before cleaning up, we need to wait for the reports to be generated
        waitForReportGeneration(crawlDir);

        try {
            executeMBeanOperation(CrawlServiceOperation.shutdown);
        } catch (IOFailure e) {
            log.error("JMX error while cleaning up Heritrix controller", e);
        }

        closeJMXConnection();

        waitForHeritrixProcessExit();
    }

    /**
     * Return the URL for monitoring this instance.
     *
     * @return the URL for monitoring this instance.
     */
    public String getAdminInterfaceUrl() {
        return "http://" + SystemUtils.getLocalHostName() + ":" + getGuiPort();
    }

    /**
     * Gets a message that stores the information summarizing the crawl
     * progress.
     *
     * @return a message that stores the information summarizing the crawl
     *         progress.
     */
    public CrawlProgressMessage getCrawlProgress() {

        HeritrixFiles files = getHeritrixFiles();
        CrawlProgressMessage cpm = new CrawlProgressMessage(files
                .getHarvestID(), files.getJobID(), progressStatisticsLegend);

        cpm.setHostUrl(getHeritrixConsoleURL());

        getCrawlServiceAttributes(cpm);
        
        if (cpm.crawlIsFinished()) {
            cpm.setStatus(CrawlStatus.CRAWLING_FINISHED);
            // No need to go further, CrawlService.Job bean does not exist
            return cpm;
        }  
        
        fetchCrawlServiceJobAttributes(cpm);
        
        return cpm;
    }
    
    /**
     * Retrieve the values of the crawl service attributes and
     * add them to the CrawlProgressMessage being put together.
     * @param cpm the crawlProgress message being prepared
     */
    private void getCrawlServiceAttributes(CrawlProgressMessage cpm) {
        List<Attribute> heritrixAtts = getMBeanAttributes(
                new CrawlServiceAttribute[] {
                CrawlServiceAttribute.AlertCount,
                CrawlServiceAttribute.IsCrawling,
                CrawlServiceAttribute.CurrentJob });

        CrawlServiceInfo hStatus = cpm.getHeritrixStatus();
        for (Attribute att : heritrixAtts) {
            Object value = att.getValue();
            CrawlServiceAttribute crawlServiceAttribute 
                = CrawlServiceAttribute.fromString(att.getName()); 
            switch (crawlServiceAttribute) {
            case AlertCount:
                Integer alertCount = -1;
                if (value != null) {
                    alertCount = (Integer) value;
                }
                hStatus.setAlertCount(alertCount);
                break;
            case CurrentJob:
                String newCurrentJob = "";
                if (value != null) {
                    newCurrentJob = (String) value;
                }
                hStatus.setCurrentJob(newCurrentJob);
                break;
            case IsCrawling:
                Boolean newCrawling = false;
                if (value != null) {
                    newCrawling = (Boolean) value;
                }
                hStatus.setCrawling(newCrawling);
                break;
            default:
                log.debug("Unhandled attribute: " + crawlServiceAttribute);
            }  
        }        
    }

    /**
     * Retrieve the values of the crawl service job attributes and
     * add them to the CrawlProgressMessage being put together.
     * @param cpm the crawlProgress message being prepared
     */
    private void fetchCrawlServiceJobAttributes(CrawlProgressMessage cpm) {
        String progressStats = (String) executeMBeanOperation(
                CrawlServiceJobOperation.progressStatistics);
        CrawlServiceJobInfo jStatus = cpm.getJobStatus();
        String newProgressStats = "?";
        if (progressStats != null) {
            newProgressStats = progressStats;
        }
        jStatus.setProgressStatistics(newProgressStats);

        if (progressStatisticsLegend == null) {
            progressStatisticsLegend = (String) executeMBeanOperation(
                    CrawlServiceJobOperation.progressStatisticsLegend);
        }
        
        List<Attribute> jobAtts = getMBeanAttributes(CrawlServiceJobAttribute
                .values());
        
        for (Attribute att : jobAtts) {
            Object value = att.getValue();
            CrawlServiceJobAttribute aCrawlServiceJobAttribute 
                = CrawlServiceJobAttribute.fromString(att.getName());
            switch (aCrawlServiceJobAttribute) {
            case CrawlTime:
                Long elapsedSeconds = -1L;
                if (value != null) {
                    elapsedSeconds = (Long) value;
                }
                jStatus.setElapsedSeconds(elapsedSeconds);
                break;
            case CurrentDocRate:
                Double processedDocsPerSec = new Double(-1L);
                if (value != null) {
                    processedDocsPerSec = (Double) value;
                }
                jStatus.setCurrentProcessedDocsPerSec(processedDocsPerSec);
                break;
            case CurrentKbRate:
                // NB Heritrix seems to store the average value in
                // KbRate instead of CurrentKbRate...
                // Inverse of doc rates.
                Long processedKBPerSec = -1L;
                if (value != null) {
                    processedKBPerSec = (Long) value;
                }
                jStatus.setProcessedKBPerSec(processedKBPerSec);
                break;
            case DiscoveredCount:
                Long discoveredCount = -1L;
                if (value != null) {
                    discoveredCount = (Long) value;
                }
                jStatus.setDiscoveredFilesCount(discoveredCount);
                break;
            case DocRate:
                Double docRate = new Double(-1L);
                if (value != null) {
                    docRate = (Double) value;
                }
                jStatus.setProcessedDocsPerSec(docRate);
                break;
            case DownloadedCount:
                Long downloadedCount = -1L;
                if (value != null) {
                    downloadedCount = (Long) value;
                }
                jStatus.setDownloadedFilesCount(downloadedCount);
                break;
            case FrontierShortReport:
                String frontierShortReport = "?";
                if (value != null) {
                    frontierShortReport = (String) value;
                }
                jStatus.setFrontierShortReport(frontierShortReport);
                break;
            case KbRate:
                // NB Heritrix seems to store the average value in
                // KbRate instead of CurrentKbRate...
                // Inverse of doc rates.
                Long kbRate = -1L;
                if (value != null) {
                    kbRate = (Long) value;
                }
                jStatus.setCurrentProcessedKBPerSec(kbRate);
                break;
            case Status:
                String newStatus = "?";
                if (value != null) {
                    newStatus = (String) value;
                }
                jStatus.setStatus(newStatus);
                if (value != null) {
                    String status = (String) value;
                    if (CrawlController.State.PAUSING.equals(status)) {
                        cpm.setStatus(CrawlStatus.CRAWLER_PAUSING);
                    } else if (CrawlController.State.PAUSED.equals(status)) {
                        cpm.setStatus(CrawlStatus.CRAWLER_PAUSED);
                    } else {
                        cpm.setStatus(CrawlStatus.CRAWLER_ACTIVE);
                    }
                }
                break;
            case ThreadCount:
                Integer currentActiveToecount = -1;
                if (value != null) {
                    currentActiveToecount = (Integer) value;
                }
                jStatus.setActiveToeCount(currentActiveToecount);
                break;
            default:
                log.debug("Unhandled attribute: " + aCrawlServiceJobAttribute);
            }
        }   
    }

    /**
     * Generates a full frontier report.
     * @return a Full frontier report.
     */
    public FullFrontierReport getFullFrontierReport() {

        return FullFrontierReport.parseContentsAsString(
                jobName,
                (String) executeOperationNoRetry(
                        crawlServiceJobBeanName,
                        CrawlServiceJobOperation.frontierReport.name(),
                        "all"));
    }

    /**
     * Get the name of the one job we let this Heritrix run. The handling of
     * done jobs depends on Heritrix not being in crawl. This call may take
     * several seconds to finish.
     *
     * @return The name of the one job that Heritrix has.
     * @throws IOFailure
     *             if the job created failed to initialize or didn't appear in
     *             time.
     * @throws IllegalState
     *             if more than one job in done list, or more than one pending
     *             job
     */
    private String getJobName() {
        /*
         * This is called just after we've told Heritrix to create a job. It may
         * take a while before the job is actually created, so we have to wait
         * around a bit.
         */
        TabularData pendingJobs = null;
        TabularData doneJobs;
        int retries = 0;
        final int maxJmxRetries = JMXUtils.getMaxTries(); 
        while (retries++ < maxJmxRetries) {
            // If the job turns up in Heritrix' pending jobs list, it's ready
            pendingJobs = (TabularData) executeMBeanOperation(
                    CrawlServiceOperation.pendingJobs);
            if (pendingJobs != null && pendingJobs.size() > 0) {
                break; // It's ready, we can move on.
            }

            // If there's an error in the job configuration, the job will be put
            // in Heritrix' completed jobs list.
            doneJobs = (TabularData) executeMBeanOperation(
                    CrawlServiceOperation.completedJobs);
            if (doneJobs != null && doneJobs.size() >= 1) {
                // Since we haven't allowed Heritrix to start any crawls yet,
                // the only way the job could have ended and then put into
                // the list of completed jobs is by error.
                if (doneJobs.size() > 1) {
                    throw new IllegalState("More than one job in done list: "
                            + doneJobs);
                } else {
                    CompositeData job = JMXUtils.getOneCompositeData(doneJobs);
                    throw new IOFailure("Job " + job + " failed: "
                            + job.get(CrawlServiceJobAttribute.Status.name()));
                }
            }
            if (retries < maxJmxRetries) {
                TimeUtils.exponentialBackoffSleep(retries);
            }
        }
        // If all went well, we now have exactly one job in the pending
        // jobs list.
        if (pendingJobs == null || pendingJobs.size() == 0) {
            throw new IOFailure("Heritrix has not created a job after "
                    + (Math.pow(2, maxJmxRetries) 
                            / TimeUtils.SECOND_IN_MILLIS)
                    + " seconds, giving up.");
        } else if (pendingJobs.size() > 1) {
            throw new IllegalState("More than one pending job: " + pendingJobs);
        } else {
            // Note that we may actually get through to here even if the job
            // is malformed. The job will then die as soon as we tell it to
            // start crawling.
            CompositeData job = JMXUtils.getOneCompositeData(pendingJobs);
            // FIXME this code does not work any more, as Heritrix no longer responds to JMX
            //String name = job.get(JmxUtils.NAME) + "-" + job.get(UID_PROPERTY);
            String name = job.get("JmxUtils.NAME") + "-" + job.get(UID_PROPERTY);
            log.info("Heritrix created a job with name " + name);
            return name;
        }
    }

    /**
     * Periodically scans the crawl dir to see if Heritrix has finished
     * generating the crawl reports. The time to wait is bounded by
     * {@link HarvesterSettings#WAIT_FOR_REPORT_GENERATION_TIMEOUT}.
     *
     * @param crawlDir
     *            the crawl directory to scan.
     */
    private void waitForReportGeneration(File crawlDir) {

        log.info("Started waiting for Heritrix report generation.");

        long currentTime = System.currentTimeMillis();
        long waitSeconds = Settings
                .getLong(HarvesterSettings.WAIT_FOR_REPORT_GENERATION_TIMEOUT);
        long waitDeadline = currentTime 
                + TimeUtils.SECOND_IN_MILLIS * waitSeconds;

        // While the deadline is not attained, periodically perform the
        // following checks:
        //    1) Verify that the crawl job MBean still exists. If not then
        //       the job is over, no need to wait more and exit the loop.
        //    2) Read the job(s status. Since Heritrix 1.14.4, a FINISHED status
        //       guarantees that all reports have been generated. In this case
        //       exit the loop.
        while (currentTime <= waitDeadline) {

            currentTime = System.currentTimeMillis();

            boolean crawlServiceJobExists = false;
            try {
                if (crawlServiceJobBeanName != null) {
                    crawlServiceJobExists =
                        getMBeanServerConnection().isRegistered(
                                JMXUtils.getBeanName(crawlServiceJobBeanName));
                } else {
                    // An error occurred when initializing the controller
                    // Simply log a warning for the record
                    log.warn("crawlServiceJobBeanName is null, earlier " 
                            + "initialization of controller did not complete.");
                }
            } catch (IOException e) {
                log.warn(e);
                continue;
            }

            if (!crawlServiceJobExists) {
                log.info(crawlServiceJobBeanName
                        + " MBean not found, report generation is finished."
                        + " Exiting wait loop.");
                break;
            }

            String status = "";
            try {
                List<Attribute> atts = getAttributesNoRetry(
                        crawlServiceJobBeanName,
                        new String[] {
                                CrawlServiceJobAttribute.Status.name() });
                status = (String) atts.get(0).getValue();
            } catch (IOFailure e) {
                log.warn(e);
                continue;
            } catch (IndexOutOfBoundsException e) {
                // sometimes the array is empty TODO find out why
                log.warn(e);
                continue;
            }

            if (CrawlController.State.FINISHED.equals(status)) {
                log.info(crawlServiceJobBeanName
                        + " status is FINISHED, report generation is complete."
                        + " Exiting wait loop.");
                return;
            }

            try {
                // Wait 20 seconds
                Thread.sleep(20 * TimeUtils.SECOND_IN_MILLIS);
            } catch (InterruptedException e) {
                log.trace("Received InterruptedException" , e);
            }
        }
        log.info("Waited " + StringUtils.formatDuration(waitSeconds)
                + " for report generation. Will proceed with cleanup.");
    }

    /**
     * Execute a single CrawlServiceOperation.
     *
     * @param operation the operation to execute
     * @param arguments any arguments needed by the operation  
     * @return Whatever the command returned.
     */
    private Object executeMBeanOperation(CrawlServiceOperation operation,
            String... arguments) {
        return executeOperation(crawlServiceBeanName, operation.name(),
                arguments);
    }

    /**
     * Execute a single CrawlServiceOperation.
     *
     * @param operation the operation to execute
     * @param arguments any arguments needed by the operation         
     * @return Whatever the command returned.
     */
    private Object executeMBeanOperation(CrawlServiceJobOperation operation,
            String... arguments) {
        return executeOperation(crawlServiceJobBeanName, operation.name(),
                arguments);
    }

    /**
     * Get the value of several attributes.
     *
     * @param attributes
     *            The attributes to get.
     * @return Whatever the command returned.
     */
    private List<Attribute> getMBeanAttributes(
            CrawlServiceJobAttribute[] attributes) {

        String[] attNames = new String[attributes.length];
        for (int i = 0; i < attributes.length; i++) {
            attNames[i] = attributes[i].name();
        }

        return getAttributes(crawlServiceJobBeanName, attNames);
    }

    /**
     * Get the value of several attributes.
     *
     * @param attributes
     *            The attributes to get.
     * @return Whatever the command returned.
     */
    private List<Attribute> getMBeanAttributes(
            CrawlServiceAttribute[] attributes) {

        String[] attNames = new String[attributes.length];
        for (int i = 0; i < attributes.length; i++) {
            attNames[i] = attributes[i].name();
        }

        return getAttributes(crawlServiceBeanName, attNames);
    }

    /**
     * Execute a command on a bean.
     *
     * @param beanName
     *            Name of the bean.
     * @param operation
     *            Command to execute.
     * @param args
     *            Arguments to the command. Only string arguments are possible
     *            at the moment.
     * @return The return value of the executed command.
     */
    private Object executeOperation(String beanName, String operation,
            String... args) {
        return jmxCall(beanName, true, true, new String[] {operation}, args);
    }

    /**
     * Execute a command on a bean, does not retry if fails.
     *
     * @param beanName
     *            Name of the bean.
     * @param operation
     *            Command to execute.
     * @param args
     *            Arguments to the command. Only string arguments are possible
     *            at the moment.
     * @return The return value of the executed command.
     */
    private Object executeOperationNoRetry(String beanName, String operation,
            String... args) {
        return jmxCall(beanName, false, true, new String[] {operation}, args);
    }

    /**
     * Get the value of several attributes from a bean.
     *
     * @param beanName
     *            Name of the bean to get an attribute for.
     * @param attributes
     *            Name of the attributes to get.
     * @return Value of the attribute.
     */
    @SuppressWarnings("unchecked")
    private List<Attribute> getAttributes(String beanName,
            String[] attributes) {
        return (List<Attribute>) jmxCall(beanName, true, false, attributes);
    }

    /**
     * Get the value of several attributes from a bean, but does not retry if
     * the fetch fails.
     *
     * @param beanName
     *            Name of the bean to get an attribute for.
     * @param attributes
     *            Name of the attributes to get.
     * @return Value of the attribute.
     */
    @SuppressWarnings("unchecked")
    private List<Attribute> getAttributesNoRetry(String beanName,
            String[] attributes) {
        return (List<Attribute>) jmxCall(beanName, false, false, attributes);
    }

    /**
     * Executes a JMX call (attribute read or single operation) on a given bean.
     *
     * @param beanName
     *            the MBean name.
     * @param retry
     *            if true, will retry a number of times if the operation fails.
     * @param isOperation
     *            true if the call is an operation, false if it's an attribute
     *            read.
     * @param names
     *            name of operation or name of attributes
     * @param args
     *            optional arguments for operations
     * @return the object returned by the distant MBean
     */
    private Object jmxCall(String beanName, boolean retry, boolean isOperation,
            String[] names, String... args) {

        MBeanServerConnection connection = getMBeanServerConnection();
        
        int maxTries = 1;
        if (retry) {
            maxTries = jmxMaxTries;
        }
        int tries = 0;
        Throwable lastException;
        do {
            tries++;
            try {
                if (isOperation) {
                    final String[] signature = new String[args.length];
                    Arrays.fill(signature, String.class.getName());
                    return connection.invoke(JMXUtils.getBeanName(beanName),
                            names[0], args, signature);
                } else {
                    return connection.getAttributes(
                            JMXUtils.getBeanName(beanName), names).asList();
                }
            } catch (IOException e) {
                lastException = e;
            } catch (ReflectionException e) {
                lastException = e;
            } catch (InstanceNotFoundException e) {
                lastException = e;
            } catch (MBeanException e) {
                lastException = e;
            }
            log.debug("Attempt " + tries + " out of " 
                    + maxTries 
                    + " attempts to make this jmxCall failed ");
            if (tries < maxTries) {
                TimeUtils.exponentialBackoffSleep(tries);
            }

        } while (tries < maxTries);

        String msg = "";
        if (isOperation) {
            msg = "Failed to execute " + names[0] + " with args "
                    + Arrays.toString(args) + " on " + beanName;
        } else {
            msg = "Failed to read attributes " + Arrays.toString(names)
                    + " of " + beanName;
        }
        
        if (lastException != null) {
            msg += ", last exception was "
                    + lastException.getClass().getName();
        }
        msg += " after " + tries + " attempts";
        throw new IOFailure(msg, lastException);
    }

    /**
     * Initializes the JMX connection.
     */
    private void initJMXConnection() {
        
        // Initialize the connection to Heritrix' MBeanServer
        this.jmxConnector = JMXUtils.getJMXConnector(SystemUtils.LOCALHOST,
                getJmxPort(), Settings
                        .get(HarvesterSettings.HERITRIX_JMX_USERNAME), Settings
                        .get(HarvesterSettings.HERITRIX_JMX_PASSWORD));
    }

    /**
     * Closes the JMX connection.
     */
    private void closeJMXConnection() {
        // Close the connection to the MBean Server
        try {
            jmxConnector.close();
        } catch (IOException e) {
            log.error("JMX error while closing connection to Heritrix", e);
        }
    }
    
    /** 
     * @return aMBeanServerConnection to Heritrix
     */
    private MBeanServerConnection getMBeanServerConnection() {
        MBeanServerConnection connection = null;
        int tries = 0;
        IOException ioe = null;
        while (tries < jmxMaxTries && connection == null) {
            tries++;
            try {
                connection = jmxConnector.getMBeanServerConnection();
                log.debug("Got a MBeanserverconnection at attempt #" + tries);
                return connection;
            } catch (IOException e) {
                ioe = e;
                log.info("IOException while getting MBeanServerConnection."
                        + " Attempt " + tries + " out of " + jmxMaxTries
                        + ". Will try to renew the JMX connection to Heritrix");
                // When an IOException is raised in RMIConnector, a terminated
                // flag is set to true, even if the underlying connection is
                // not closed. This seems to be part of a mechanism to prevent
                // deadlocks, but can cause trouble for us.
                // So if this happens, we close and reinitialize
                // the JMX connector itself.
                closeJMXConnection();
                try {
                    initJMXConnection();
                    log.info("Successfully renewed JMX connection");
                } catch (IOFailure e1) {
                    log.debug(
                            "Renewal of JMXConnection failed at retry #" 
                                    + tries + " with exception: ", e1);
                    }
                }
            if (tries < jmxMaxTries) {
                TimeUtils.exponentialBackoffSleep(tries);
            }
        }

        if (ABORT_IF_CONN_LOST) {
            log.debug("Connection to Heritrix seems to be lost. "
                    + "Trying to abort ...");
            throw new HarvestingAbort("Failed to connect to MBeanServer", ioe);
        } else {
            throw new IOFailure("Failed to connect to MBeanServer", ioe);
        }
    }

    @Override
    public boolean atFinish() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public void beginCrawlStop() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public void cleanup() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public boolean crawlIsEnded() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public int getActiveToeCount() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public int getCurrentProcessedKBPerSec() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public String getHarvestInformation() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public String getProgressStats() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public long getQueuedUriCount() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public boolean isPaused() {
        throw new NotImplementedException("Not implemented");
    }

}
