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
package dk.netarkivet.harvester;

import java.util.regex.Pattern;

import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.harvesting.HarvestDocumentation;
import dk.netarkivet.harvester.harvesting.controller.BnfHeritrixControllerBasedRestController;
import dk.netarkivet.harvester.harvesting.distribute.HarvesterReadyMessage;
import dk.netarkivet.harvester.harvesting.frontier.TopTotalEnqueuesFilter;
import dk.netarkivet.harvester.harvesting.report.HarvestReport;
import dk.netarkivet.harvester.scheduler.JobDispatcher;
import dk.netarkivet.harvester.scheduler.jobgen.FixedDomainConfigurationCountJobGenerator;
import dk.netarkivet.harvester.scheduler.jobgen.DefaultJobGenerator;

/** Settings specific to the harvester module of NetarchiveSuite. */
public class HarvesterSettings {
    /** The default place in classpath where the settings file can be found. */
    private static final String DEFAULT_SETTINGS_CLASSPATH
            = "dk/netarkivet/harvester/settings.xml";

    /*
     * The static initialiser is called when the class is loaded.
     * It will add default values for all settings defined in this class, by
     * loading them from a settings.xml file in classpath.
     */
    static {
        Settings.addDefaultClasspathSettings(
                DEFAULT_SETTINGS_CLASSPATH

        );
    }

    // NOTE: The constants defining setting names below are left non-final on
    // purpose! Otherwise, the static initialiser that loads default values
    // will not run.

    /**
     * <b>settings.harvester.datamodel.domain.defaultSeedlist</b>: <br> Default
     * name of the seedlist to use when new domains are created.
     */
    public static String DEFAULT_SEEDLIST
            = "settings.harvester.datamodel.domain.defaultSeedlist";

    /**
     * <b>settings.harvester.datamodel.domain.validSeedRegex</b>: <br>
     * Regular expression used to validate a seed within a seedlist.
     *
     * Default value accepts all non-empty strings.
     */
    public static String VALID_SEED_REGEX
            = "settings.harvester.datamodel.domain.validSeedRegex";

    /**
     * <b>settings.harvester.datamodel.domain.defaultConfig</b>: <br> The name
     * of a configuration that is created by default and which is initially used
     * for snapshot harvests.
     */
    public static String DOMAIN_DEFAULT_CONFIG
            = "settings.harvester.datamodel.domain.defaultConfig";

    /**
     * <b>settings.harvester.datamodel.domain.defaultOrderxml</b>: <br> Name of
     * order xml template used for domains if nothing else is specified. The
     * newly created configurations use this. This template must exist before
     * harvesting can commence
     */
    public static String DOMAIN_DEFAULT_ORDERXML
            = "settings.harvester.datamodel.domain.defaultOrderxml";

    /**
     * <b>settings.harvester.datamodel.domain.defaultMaxrate</b>: <br> Default
     * download rate for domain configuration. Not currently enforced.
     */
    public static String DOMAIN_CONFIG_MAXRATE
            = "settings.harvester.datamodel.domain.defaultMaxrate";

    /**
     * <b>settings.harvester.datamodel.domain.defaultMaxbytes</b>: <br> Default
     * byte limit for domain configuration.
     */
    public static String DOMAIN_CONFIG_MAXBYTES
            = "settings.harvester.datamodel.domain.defaultMaxbytes";

    /**
     * <b>settings.harvester.datamodel.domain.defaultMaxobjects</b>: <br>
     * Default object limit for domain configuration.
     */
    public static String DOMAIN_CONFIG_MAXOBJECTS
            = "settings.harvester.datamodel.domain.defaultMaxobjects";

    /**
     * <b>settings.harvester.scheduler.jobGen.config.errorFactorPrevResult</b>: <br>
     * Used when calculating expected size of a harvest of some configuration during
     * job-creation process. This defines how great a possible factor we will
     * permit a harvest to be larger then the expectation, when basing the
     * expectation on a previous completed job.
     */
    public static String ERRORFACTOR_PERMITTED_PREVRESULT
            = "settings.harvester.scheduler.jobGen.config.errorFactorPrevResult";

    /**
     * <b>settings.harvester.scheduler.jobGen.config.errorFactorBestGuess</b>: <br> Used when
     * calculating expected size of a harvest of some configuration during
     * job-creation process. This defines how great a possible factor we will
     * permit a harvest to be larger then the expectation, when basing the
     * expectation on previous uncompleted harvests or no harvest data at all.
     */
    public static String ERRORFACTOR_PERMITTED_BESTGUESS
            = "settings.harvester.scheduler.jobGen.config.errorFactorBestGuess";

    /**
     * <b>settings.harvester.scheduler.jobGen.config.expectedAverageBytesPerObject</b>: <br>
     * How many bytes the average object is expected to be on domains where we
     * don't know any better.  This number should grow over time, as of end of
     * 2005 empirical data shows 38000.
     */
    public static String EXPECTED_AVERAGE_BYTES_PER_OBJECT
            = "settings.harvester.scheduler.jobGen.config.expectedAverageBytesPerObject";

    /**
     * <b>settings.harvester.scheduler.jobGen.config.maxDomainSize</b>: <br> The initial guess
     * of the domain size (number of objects) of an unknown domain.
     */
    public static String MAX_DOMAIN_SIZE
            = "settings.harvester.scheduler.jobGen.config.maxDomainSize";

    /**
     * <b>settings.harvester.scheduler.jobGen.config.maxRelativeSizeDifference</b>: <br>
     * The maximum allowed relative difference in expected number of objects
     * retrieved in a single job definition. To avoid job splitting, set the
     * value as Long.MAX_VALUE.
     */
    public static String JOBS_MAX_RELATIVE_SIZE_DIFFERENCE
            = "settings.harvester.scheduler.jobGen.config.maxRelativeSizeDifference";

    /**
     * <b>settings.harvester.scheduler.jobGen.config.minAbsoluteSizeDifference</b>: <br>
     * Size differences for jobs below this threshold are ignored, regardless of
     * the limits for the relative size difference. To avoid job splitting, set
     * the value as Long.MAX_VALUE.
     */
    public static String JOBS_MIN_ABSOLUTE_SIZE_DIFFERENCE
            = "settings.harvester.scheduler.jobGen.config.minAbsoluteSizeDifference";

    /**
     * <b>settings.harvester.scheduler.jobGen.config.maxTotalSize</b>: <br> When this
     * limit is exceeded no more configurations may be added to a job. To avoid
     * job splitting, set the value as Long.MAX_VALUE.
     */
    public static String JOBS_MAX_TOTAL_JOBSIZE
            = "settings.harvester.scheduler.jobGen.config.maxTotalSize";

    /**
     * <b>settings.harvester.scheduler.jobGen.maxTimeToCompleteJob</b>:
     * <br> The limit on how many seconds Heritrix should continue on
     * each job. O means no limit.
     */
    public static String JOBS_MAX_TIME_TO_COMPLETE
            = "settings.harvester.scheduler.jobGen.maxTimeToCompleteJob";

    /**
     * <b>settings.harvester.scheduler.jobGen.domainConfigSubsetSize</b>: <br>
     * How many domain configurations we will process in one go before making jobs out
     * of them. This amount of domains will be stored in memory at the same time.
     * To avoid job splitting, set this value as Long.MAX_VALUE.
     */
    public static String JOBGEN_DOMAIN_CONFIG_SUBSET_SIZE
            = "settings.harvester.scheduler.jobGen.domainConfigSubsetSize";

    /**
     * <b>settings.harvester.scheduler.jobGen.config.fixedDomainCountFocused</b>: <br>
     * If the job generator is {@link FixedDomainConfigurationCountJobGenerator},
     * then this parameter represents the maximum number of domain configurations
     * in a partial harvest job.
     */
    public static String JOBGEN_FIXED_CONFIG_COUNT_FOCUSED
            = "settings.harvester.scheduler.jobGen.config.fixedDomainCountFocused";

    /**
     * <b>settings.harvester.scheduler.jobGen.config.fixedDomainCountSnapshot</b>: <br>
     * If the job generator is {@link FixedDomainConfigurationCountJobGenerator},
     * then this parameter represents the maximum number of domain configurations
     * in a full harvest job.
     */
    public static String JOBGEN_FIXED_CONFIG_COUNT_SNAPSHOT
            = "settings.harvester.scheduler.jobGen.config.fixedDomainCountSnapshot";

    /**
     * <b>settings.harvester.scheduler.jobGen.class</b>: <br>
     * The fully qualified class name of the chosen job generator implementation,
     * currently either {@link DefaultJobGenerator}
     * or {@link FixedDomainConfigurationCountJobGenerator}.
     * The default is {@link DefaultJobGenerator}.
     */
    public static String JOBGEN_CLASS = "settings.harvester.scheduler.jobGen.class";

    /**
     * <b>settings.harvester.scheduler.jobGen.config.splitByObjectLimit</b>: <br> By default
     * the byte limit is used as the base criterion for how many domain
     * configurations are put into one harvest job. However if this parameter is
     * set to "true", then the object limit is used instead as the base
     * criterion.
     */
    public static String SPLIT_BY_OBJECTLIMIT =
            "settings.harvester.scheduler.jobGen.config.splitByObjectLimit";

    /**
     * <b>settings.harvester.scheduler.jobGen.objectLimitIsSetByQuotaEnforcer</b>: <br>
     * Controls whether the domain configuration object limit should be set in
     * Heritrix's crawl order through the QuotaEnforcer configuration (parameter
     * set to true) or through the frontier parameter 'queue-total-budget' (
     * parameter set to false).
     *
     * Default value is true, as legacy implementation was to use only
     * the QuotaEnforcer.
     */
    public static String OBJECT_LIMIT_SET_BY_QUOTA_ENFORCER =
            "settings.harvester.scheduler.jobGen.objectLimitIsSetByQuotaEnforcer";

    /**
     * <b>settings.harvester.scheduler.jobtimeouttime</b>:<br /> Time before a
     * STARTED job times out and change status to FAILED. In seconds.
     */
    public static String JOB_TIMEOUT_TIME =
            "settings.harvester.scheduler.jobtimeouttime";

    /**
     * <b>settings.harvester.scheduler.jobgenerationperiode</b>: <br>
     * The period between checking if new jobs should be generated, in seconds.
     * This is one minute because that's the finest we can define in a harvest
     * definition.
     */
    public static String GENERATE_JOBS_PERIOD =
        "settings.harvester.scheduler.jobgenerationperiode";

    /**
     * <b>settings.harvester.harvesting.serverDir</b>: <br> Each job gets a
     * subdir of this dir. Job data is written and Heritrix writes to that
     * subdir.
     */
    public static String HARVEST_CONTROLLER_SERVERDIR
            = "settings.harvester.harvesting.serverDir";

    /**
     * <b>settings.harvester.harvesting.minSpaceLeft</b>: <br> The minimum
     * amount of free bytes in the serverDir required before accepting any
     * harvest-jobs.
     */
    public static String HARVEST_SERVERDIR_MINSPACE
            = "settings.harvester.harvesting.minSpaceLeft";

    /**
     * <b>settings.harvester.harvesting.oldjobsDir</b>: <br> The directory in
     * which data from old jobs is kept after uploading. Each directory from
     * serverDir will be moved to here if any data remains, either due to failed
     * uploads or because it wasn't attempted uploaded.
     */
    public static String HARVEST_CONTROLLER_OLDJOBSDIR
            = "settings.harvester.harvesting.oldjobsDir";

    /**
     * <b>settings.harvester.harvesting.queuePriority</b>: <br> Pool to take
     * jobs from. There are two pools to choose from, labelled "HIGHPRIORITY"
     * (pool for selective harvest jobs), and "LOWPRIORITY" (pool for snapshot
     * harvest jobs) respectively.
     *
     * NOTE: this one is also used in SingleMBeanObject parsing information to
     * System state
     */
    public static String HARVEST_CONTROLLER_PRIORITY
            = "settings.harvester.harvesting.queuePriority";

    /**
     * <b>settings.harvester.harvesting.heritrix.inactivityTimeout</b>: <br> The
     * timeout setting for aborting a crawl based on crawler-inactivity. If the
     * crawler is inactive for this amount of seconds the crawl will be aborted.
     * The inactivity is measured on the crawlController.activeToeCount().
     */
    public static String INACTIVITY_TIMEOUT_IN_SECS
            = "settings.harvester.harvesting.heritrix.inactivityTimeout";

    /**
     * <b>settings.harvester.harvesting.heritrix.noresponseTimeout</b>: <br> The
     * timeout value (in seconds) used in HeritrixLauncher for aborting crawl
     * when no bytes are being received from web servers.
     */
    public static String CRAWLER_TIMEOUT_NON_RESPONDING
            = "settings.harvester.harvesting.heritrix.noresponseTimeout";
    /**
     * <b>settings.harvester.monitor.refreshInterval</b>:<br>
     * Time interval in seconds after which the harvest monitor pages will be
     * automatically refreshed.
     */
    public static String HARVEST_MONITOR_REFRESH_INTERVAL =
        "settings.harvester.monitor.refreshInterval";

    /**
     * <b>settings.harvester.monitor.historySampleRate</b>:<br>
     * Time interval in seconds between historical records stores in the DB.
     * Default value is 5 minutes.
     */
    public static String HARVEST_MONITOR_HISTORY_SAMPLE_RATE =
        "settings.harvester.monitor.historySampleRate";

    /**
     * <b>settings.harvester.monitor.historyChartGenIntervall</b>:<br>
     * Time interval in seconds between regenerating the chart of historical
     * data for a running job.
     * Default value is 5 minutes.
     */
    public static String HARVEST_MONITOR_HISTORY_CHART_GEN_INTERVAL =
        "settings.harvester.monitor.historyChartGenInterval";

    /**
     * <b>settings.harvester.monitor.displayedHistorySize</b>:<br>
     * Maximum number of most recent history records displayed on the
     * running job details page.
     */
    public static String HARVEST_MONITOR_DISPLAYED_HISTORY_SIZE =
        "settings.harvester.monitor.displayedHistorySize";

    /**
     * <b>settings.harvester.harvesting.heritrix.crawlLoopWaitTime</b>:<br>
     * Time interval in seconds to wait during a crawl loop in the
     * harvest controller. Default value is 20 seconds.
     */
    public static String CRAWL_LOOP_WAIT_TIME =
        "settings.harvester.harvesting.heritrix.crawlLoopWaitTime";

    /**
     * <b>settings.harvester.harvesting.sendReadyInterval</b>:<br>
     * Time interval in seconds to wait before transmitting a
     * {@link HarvesterReadyMessage} to the {@link JobDispatcher}.<p>
     * 
     * Lower values will make the JobDispatcher detect ready harvester
     * faster, but will make it more likely that the harvester may send two 
     * ready messages before a job is received, causing the JobDispatcher to 
     * dispatch two jobs.
     * 
     * Default value is 30 second.
     */
    public static String SEND_READY_INTERVAL =
        "settings.harvester.harvesting.sendReadyInterval";

    /**
     * <b>settings.harvester.harvesting.sendReadyDelay</b>:<br>
     * Time in milliseconds to wait from starting to listen on the job queue to 
     * a potential ready message is sent to the HarvestJobManager. This small 
     * delay is used to retrieve any left over jobs on the queue before sending 
     * the ready message to the harvester.
     * Default value is 1000 millisecond.
     */
    public static String SEND_READY_DELAY =
        "settings.harvester.harvesting.sendReadyDelay";
    
    /**
     * <b>settings.harvester.harvesting.frontier.frontierReportWaitTime</b>:<br>
     * Time interval in seconds to wait between two requests to generate a full
     * frontier report. Default value is 600 seconds (10 min).
     */
    public static String FRONTIER_REPORT_WAIT_TIME =
        "settings.harvester.harvesting.frontier.frontierReportWaitTime";

    /**
     * <b>settings.harvester.harvesting.frontier.filter.class</b>
     * Defines a filter to apply to the full frontier report.
     * the default class: {@link TopTotalEnqueuesFilter}
     */
    public static String FRONTIER_REPORT_FILTER_CLASS =
            "settings.harvester.harvesting.frontier.filter.class";

    /**
     * <b>settings.harvester.harvesting.frontier.filter.args</b>
     * Defines a frontier report filter's arguments. Arguments should be
     * separated by semicolons.
     */
    public static String FRONTIER_REPORT_FILTER_ARGS =
            "settings.harvester.harvesting.frontier.filter.args";

    /**
     * <b>settings.harvester.harvesting.heritrix.abortIfConnectionLost</b>:<br>
     * Boolean flag. If set to true, the harvest controller will abort the
     * current crawl when the JMX connection is lost. If set to true it will
     * only log a warning, leaving the crawl operator shutting down harvester
     * manually.
     * Default value is true.
     * @see BnfHeritrixControllerBasedRestController
     */
    public static String ABORT_IF_CONNECTION_LOST =
        "settings.harvester.harvesting.heritrix.abortIfConnectionLost";

    /**
     * <b>settings.harvester.harvesting.heritrix.waitForReportGenerationTimeout</b>:<br>
     * Maximum time in seconds to wait for Heritrix to generate report files
     * once crawling is over.
     */
    public static String WAIT_FOR_REPORT_GENERATION_TIMEOUT =
        "settings.harvester.harvesting.heritrix.waitForReportGenerationTimeout";

    /**
     * <b>settings.harvester.harvesting.heritrix.adminName</b>: <br> The name
     * used to access the Heritrix GUI.
     */
    public static String HERITRIX_ADMIN_NAME
            = "settings.harvester.harvesting.heritrix.adminName";

    /**
     * <b>settings.harvester.harvesting.heritrix.adminPassword</b>: <br> The
     * password used to access the Heritrix GUI.
     */
    public static String HERITRIX_ADMIN_PASSWORD
            = "settings.harvester.harvesting.heritrix.adminPassword";

    /**
     * <b>settings.harvester.harvesting.heritrix.guiPort</b>: <br> Port used to
     * access the Heritrix web user interface. This port must not be used by
     * anything else on the machine. Note that apart from pausing a job,
     * modifications done directly on Heritrix may cause unexpected breakage.
     */
    public static String HERITRIX_GUI_PORT
            = "settings.harvester.harvesting.heritrix.guiPort";

    /**
     * <b>settings.harvester.harvesting.heritrix.jmxPort</b>: <br> The port that
     * Heritrix uses to expose its JMX interface. This port must not be used by
     * anything else on the machine, but does not need to be accessible from
     * other machines unless you want to be able to use jconsole to access
     * Heritrix directly. Note that apart from pausing a job, modifications done
     * directly on Heritrix may cause unexpected breakage.
     */
    public static String HERITRIX_JMX_PORT
            = "settings.harvester.harvesting.heritrix.jmxPort";

    /**
     * <b>settings.harvester.harvesting.heritrix.jmxUsername</b>: <br> The
     * username used to connect to Heritrix JMX interface The username must
     * correspond to the value stored in the jmxremote.password file (name
     * defined in setting settings.common.jmx.passwordFile).
     */
    public static String HERITRIX_JMX_USERNAME
            = "settings.harvester.harvesting.heritrix.jmxUsername";

    /**
     * <b>settings.harvester.harvesting.heritrix.jmxPassword</b>: <br> The
     * password used to connect to Heritrix JMX interface The password must
     * correspond to the value stored in the jmxremote.password file (name
     * defined in setting settings.common.jmx.passwordFile).
     */
    public static String HERITRIX_JMX_PASSWORD
            = "settings.harvester.harvesting.heritrix.jmxPassword";

    /**
     * <b>settings.harvester.harvesting.heritrix.heapSize</b>: <br> The heap
     * size to use for the Heritrix sub-process.  This should probably be fairly
     * large. It can be specified in the same way as for the -Xmx argument to
     * Java, e.g. 512M, 2G etc.
     */
    public static String HERITRIX_HEAP_SIZE
            = "settings.harvester.harvesting.heritrix.heapSize";

    /**
     * <b>settings.harvester.harvesting.heritrix.javaOpts</b>: <br> Additional
     * JVM options for the Heritrix sub-process. By default there is no
     * additional JVM option.
     */
    public static String HERITRIX_JVM_OPTS =
            "settings.harvester.harvesting.heritrix.javaOpts";

    /**
     * <b>settings.harvester.harvesting.heritrixControllerClass</b>:<br/> The
     * implementation of the HeritrixController interface to be used.
     */
    public static String HERITRIX_CONTROLLER_CLASS =
            "settings.harvester.harvesting.heritrixController.class";

    /**
     * <b>settings.harvester.harvesting.heritrixLauncherClass</b>:<br/> The
     * implementation of the HeritrixLauncher abstract class to be used.
     */
    public static String HERITRIX_LAUNCHER_CLASS =
            "settings.harvester.harvesting.heritrixLauncher.class";

    /**
     * <b>settings.harvester.harvesting.harvestReport</b>:<br/> The
     * implementation of {@link HarvestReport} interface to be used.
     */
    public static String HARVEST_REPORT_CLASS =
            "settings.harvester.harvesting.harvestReport.class";
    
    /**
     * <b>settings.harvester.harvesting.harvestReport.disregardSeedsURLInfo</b>:<br/> 
     * Should we disregard seedURL-information and thus assign the harvested bytes to 
     * the domain of the harvested URL instead of the seed url domain?
     * The default is false;
     */
    public static String DISREGARD_SEEDURL_INFORMATION_IN_CRAWLLOG =
            "settings.harvester.harvesting.harvestReport.disregardSeedURLInfo";
    
    /**
     * <b>settings.harvester.harvesting.deduplication.enabled</b>:<br/> This
     * setting tells the system whether or not to use deduplication. This
     * setting is true by default.
     */
    public static String DEDUPLICATION_ENABLED =
            "settings.harvester.harvesting.deduplication.enabled";

    /**
     * <b>settings.harvester.harvesting.metadata.heritrixFilePattern</b> This
     * setting allows to filter which Heritrix files should be stored in the
     * metadata ARC.
     *
     * @see Pattern
     */
    public static String METADATA_HERITRIX_FILE_PATTERN =
            "settings.harvester.harvesting.metadata.heritrixFilePattern";

    /**
     * <b>settings.harvester.harvesting.metadata.reportFilePattern</b> This
     * setting allows to filter which Heritrix files that should be stored in
     * the metadata ARC are to be classified as a report.
     *
     * @see Pattern
     */
    public static String METADATA_REPORT_FILE_PATTERN =
            "settings.harvester.harvesting.metadata.reportFilePattern";

    /**
     * <b>settings.harvester.harvesting.metadata.logFilePattern</b> This setting
     * allows to filter which Heritrix log files should be stored in the
     * metadata ARC.
     *
     * @see Pattern
     */
    public static String METADATA_LOG_FILE_PATTERN =
            "settings.harvester.harvesting.metadata.logFilePattern";

    /**
     * <b>settings.harvester.harvesting.metadata.generateArcFilesReport</b> This setting
     * is a boolean flag that enables/disables the generation of an ARC files report.
     * Default value is 'true'.
     *
     * @see HarvestDocumentation#documentHarvest(java.io.File, long, long)
     */
    public static String METADATA_GENERATE_ARCFILES_REPORT =
            "settings.harvester.harvesting.metadata.generateArcFilesReport";

    /**
     * <b>settings.harvester.aliases.timeout</b> The amount of time in seconds before 
     * an alias times out, and needs to be re-evaluated.
     * The default value is one year, i.e 31536000 seconds.
     */
    public static String ALIAS_TIMEOUT = "settings.harvester.aliases.timeout";
    
    /**
     * <b>settings.harvester.harvesting.continuationFromHeritrixRecoverlogEnabled</b>:</br> 
     * Setting for whether or not a restarted job should try fetching the recoverlog 
     * of the previous failed job, and ask Heritrix to continue from this log. 
     * The default is false.
     */
    public static String RECOVERlOG_CONTINUATION_ENABLED 
        = "settings.harvester.harvesting.continuationFromHeritrixRecoverlogEnabled";
    
    /**
     * <b>settings.harvester.harvesting.metadata.metadataFormat</b>  The dataformat used by Netarchivesuite to write the metadata associated
     * with a given harvest job.
     * default: arc (alternative: warc) 
     */
    public static String METADATA_FORMAT =
            "settings.harvester.harvesting.metadata.metadataFormat";

    /**
     * <b>settings.harvester.harvesting.heritrix.archiveFormat</b> The dataformat used by heritrix to write the harvested data.
     * default: arc (alternative: warc) 
     */
    public static String HERITRIX_ARCHIVE_FORMAT =
            "settings.harvester.harvesting.heritrix.archiveFormat";
    /**
     * <b>settings.harvester.harvesting.heritrix.archiveNaming.class</b> The class implementing the chosen way of naming your archive-files
     * default: LegacyNamingConvention.
     * This class decides what to put into the Heritrix "prefix" property of the org.archive.crawler.writer.ARCWriterProcessor and/or 
     * org.archive.crawler.writer.WARCWriterProcessor.
     */
    public static String HERITRIX_ARCHIVE_NAMING_CLASS =
            "settings.harvester.harvesting.heritrix.archiveNaming.class";
    
    /** 
     * <b>settings.harvester.performer</b>: <br>The agent performing these harvests. 
     * The default is: "" */
    public static String PERFORMER = "settings.harvester.performer";
    

    /***************************/
    /* Indexserver - settings. */
    /***************************/
    
    /**
     * <b>settings.harvester.indexserver.requestdir</b>: <br>
     * Setting for where the requests of the indexserver are stored.
     */
    public static String INDEXSERVER_INDEXING_REQUESTDIR
            = "settings.harvester.indexserver.requestdir";
    
    /**
     * <b>settings.harvester.indexserver.maxclients</b>: <br>
     * Setting for the max number of clients the indexserver can handle 
     * simultaneously.
     */
    public static String INDEXSERVER_INDEXING_MAXCLIENTS
            = "settings.harvester.indexserver.maxclients";   
    
    /**
     * <b>settings.harvester.indexserver.maxthreads</b>: <br>
     * Setting for the max number of threads the deduplication indexer 
     * shall use.
     */
    public static String INDEXSERVER_INDEXING_MAXTHREADS
            = "settings.harvester.indexserver.maxthreads";
    /**
     * <b>settings.harvester.indexserver.checkinterval</b>: <br>
     * Setting for the time in milliseconds between each 
     * check of the state of sub-indexing.
     * Default: 30 seconds (30000 milliseconds).
     */
    public static String INDEXSERVER_INDEXING_CHECKINTERVAL
            = "settings.harvester.indexserver.checkinterval";
    
    /**
     * <b>settings.harvester.indexserver.indexingtimeout</b>: <br>
     * Setting for the indexing timeout in milliseconds. The default is
     * 259200000 (3 days).
     */
    public static String INDEXSERVER_INDEXING_TIMEOUT
            = "settings.harvester.indexserver.indexingtimeout";
    
    /**
     * <b>settings.harvester.indexserver.maxsegments</b>: <br>
     * Setting for how many segments we will accept in our lucene indices.
     * The default is 15.
     */
    public static String INDEXSERVER_INDEXING_MAX_SEGMENTS
            = "settings.harvester.indexserver.maxsegments";
    
    /**
     * <b>settings.harvester.indexserver.listeningcheckinterval</b>: <br>
     * Setting for the interval between each listening check in milliseconds. 
     * The default is 30000 (5 minutes).
     */
    public static String INDEXSERVER_INDEXING_LISTENING_INTERVAL 
        = "settings.harvester.indexserver.listeningcheckinterval";
    /**
     * <b>settings.archive.indexserver.satisfactorythresholdpercentage</b>: <br>
     * Setting for the satisfactory threshold of the indexing result as a percentage.
     * The default is 70 percent
     */
    public static String INDEXSERVER_INDEXING_SATISFACTORYTHRESHOLD_PERCENTAGE
        = "settings.harvester.indexserver.satisfactorythresholdpercentage";
    
    /**
     * <b>settings.harvester.indexserver.indexrequestserver.class</b>: <br>
     * Setting for which type of indexrequestserver to use.
     * The default is: 
     * {@link dk.netarkivet.harvester.indexserver.distribute.IndexRequestServer}
     */
    public static String INDEXREQUEST_SERVER_CLASS
        = "settings.harvester.indexserver.indexrequestserver.class";
    
    /**
     * b>settings.harvester.indexserver.lookfordataInAllBitarchiveReplicas</b>: <br>
     * Setting for whether or not data not found in the default bitarchive replica
     * shall be looked for in other bitarchive replicas.
     * The default is false.
     */
    public static String INDEXSERVER_INDEXING_LOOKFORDATAINOTHERBITARCHIVEREPLICAS
        = "settings.harvester.indexserver.lookfordataInAllBitarchiveReplicas";

    
    /***************************/
    /* Viewerproxy - settings. */
    /***************************/
    
    /**
     * <b>settings.viewerproxy.baseDir</b>: <br>
     * The main directory for the ViewerProxy, used for storing the Lucene
     * index for the jobs being viewed. This directory can be used by multiple
     * ViewerProxy applications running on the same machine. */
    public static String VIEWERPROXY_DIR = "settings.harvester.viewerproxy.baseDir";

    /**
     * <b>settings.viewerproxy.tryLookupUriAsFtp</b>: <br>
     * If we fail to lookup an URI, we will try changing the protocol to ftp,
     * if this setting is set to true. The default is false.
     *
     */
    public static String TRY_LOOKUP_URI_AS_FTP = "settings.harvester.viewerproxy.tryLookupUriAsFtp";

    /**
     * <b>settings.viewerproxy.maxSizeInBrowser</b>
     * The size (in bytes) of the largest object to be returned for viewing in
     * the browser window. Larger objects will be returned with the
     * appropriate http header for saving them to a file.
     */
    public static String MAXIMUM_OBJECT_IN_BROWSER = "settings.harvester.viewerproxy.maxSizeInBrowser";

    /**
     * The maximum length (in lines) of crawllog to be displayed in a browser window.
     */
    public static String MAX_CRAWLLOG_IN_BROWSER = "settings.harvester.webinterface.maxCrawlLogInBrowser";
    
}

