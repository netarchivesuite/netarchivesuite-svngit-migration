package dk.netarkivet.harvester.harvesting.rest;

import org.apache.http.HttpResponse;
import org.w3c.dom.Document;

/**
 * Interface for the Heritrix REST API.
 *
 * @author truemped@googlemail.com
 */
public interface HeritrixSession {

    /**
     * Update the job's configuration.
     * 
     * @param jobName Name of the Heritrix Job.
     * @param cXml The new Job configuration as XML.
     */
    void updateConfig(String jobName, String cXml);

    /**
     * Checkpoint a job.
     *
     * @param jobName Name of the Heritrix Job.
     */
    void checkpointJob(String jobName);

    /**
     * Tear down a job.
     *
     * @param jobName Name of the Heritrix Job.
     */
    void tearDownJob(String jobName);

    /**
     * Terminate a job.
     *
     * @param jobName Name of the Heritrix Job.
     */
    void terminateJob(String jobName);

    /**
     * Launch a job.
     *
     * @param jobName Name of the Heritrix Job.
     */
    boolean launchJob(String jobName);

    /**
     * Build a job.
     *
     * @param jobName Name of the Heritrix Job.
     */
    boolean buildJob(String jobName);

    /**
     * Unpause a job.
     *
     * @param jobName Name of the Heritrix Job.
     */
    void unpauseJob(String jobName);

    /**
     * Pause a job.
     *
     * @param jobName Name of the Heritrix Job.
     */
    void pauseJob(String jobName);

    /**
     * Rescan the job directory.
     * 
     * @return TODO
     */
    Document rescanJobDirectory();

    /**
     * Create a new job.
     * 
     * @param jobName Name of the Heritrix Job.
     */
    void createJob(String jobName);

    /**
     * Check if a job is running.
     * 
     * @param jobName Name of the Heritrix Job.
     * @return <b>true</b> if the job is running.
     */
    boolean isJobRunning(String jobName);

    /**
     * Return a job's status description.
     *
     * @param jobName Name of the Heritrix Job.
     * @return The parsed XML.
     */
    Document getJobStatus(String jobName);

    /**
     * Return the complete crawl log.
     * 
     * @param jobName Name of the Heritrix Job.
     * @return The crawlLog as a {@link HttpResponse}.
     */
    HttpResponse getCrawlLog(String jobName);

    /**
     * Check if a job exists.
     *
     * @param jobName Name of the Heritrix Job.
     * @return <b>true</b> if the job exists.
     */
    boolean jobExists(String jobName);

    /**
     * Produces a job with the jobName which is a copy of the original.
     *
     * @param original The name of the original job.
     * @param jobName Name of the Heritrix Job.
     * @param asProfile <b>true</b> if there is no sample and the job hast to be constructed based on a profile.
     */
    void copyJob(String original, String jobName, boolean asProfile);

    /**
     * @param jobName Name of the Heritrix Job.
     * @return <b>true</b> if the job is paused.
     */
    boolean isPaused(String jobName);

}
