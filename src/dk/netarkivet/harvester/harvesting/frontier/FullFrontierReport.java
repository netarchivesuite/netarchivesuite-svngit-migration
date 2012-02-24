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
package dk.netarkivet.harvester.harvesting.frontier;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.*;
import com.sleepycat.persist.model.*;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.Iterator;

/**
 * Wraps an Heritrix full frontier report.
 * As these reports can be big in size, this implementation relies on
 * Berkeley DB direct persistence layer to store the report lines, allowing to
 * store the lines partially in memory, and on disk.
 */
public class FullFrontierReport extends AbstractFrontierReport {

    @Persistent
    static class PersistentLineKey
    implements Comparable<PersistentLineKey>, FrontierReportLineOrderKey {

        @KeyField(1)
        long totalEnqueues;

        @KeyField(2)
        String domainName;

        // Default empty constructor for BDB.
        PersistentLineKey() {

        }

        public PersistentLineKey(FrontierReportLine l) {
            this.domainName = l.getDomainName();
            this.totalEnqueues = l.getTotalEnqueues();
        }

        public String getQueueId() {
            return domainName;
        }

        public long getQueueSize() {
            return totalEnqueues;
        }

        /**
         * Compares first by decreasing queue size, then by domain name.
         */
        @Override
        public int compareTo(PersistentLineKey k) {
            return FrontierReportLineNaturalOrder.getInstance()
                .compare(this, k);
        }

        @Override
        public String toString() {
            return totalEnqueues + " " + domainName;
        }

    }

    @Entity
    static class PersistentLine extends FrontierReportLine {

        @PrimaryKey
        private PersistentLineKey primaryKey;

        @SecondaryKey(relate=Relationship.ONE_TO_ONE)
        private String domainNameKey;

        @SecondaryKey(relate=Relationship.MANY_TO_ONE)
        private Long totalSpendKey;

        @SecondaryKey(relate=Relationship.MANY_TO_ONE)
        private Long currentSizeKey;

        // Default empty constructor for BDB.
        PersistentLine() {

        }

        PersistentLine(FrontierReportLine reportLine) {
            super(reportLine);
            this.primaryKey = new PersistentLineKey(reportLine);
            this.domainNameKey = reportLine.getDomainName();
            this.currentSizeKey = reportLine.getCurrentSize();
            this.totalSpendKey = reportLine.getTotalSpend();
        }

    }

    public class ReportIterator implements Iterator<FrontierReportLine> {

        private final EntityCursor<PersistentLine> cursor;
        private final Iterator<PersistentLine> iter;

        /**
         * Returns an iterator on the given sort key.
         * @param cursor The cursor (sort key) to iterate on.
         */
        ReportIterator(EntityCursor<PersistentLine> cursor) {
            this.cursor = cursor;
            iter = cursor.iterator();
        }

        @Override
        public boolean hasNext() {
            return iter.hasNext();
        }

        @Override
        public FrontierReportLine next() {
            return iter.next();
        }

        @Override
        public void remove() {
            throw new ArgumentNotValid("Remove is not supported!");
        }

        /**
         * Close method should be called explicitely to free underlying
         * resources!
         */
        public void close() {
            try {
                cursor.close();
            } catch (DatabaseException e) {
                LOG.error("Error closing entity cursor:\n"
                        + e.getLocalizedMessage());
            }
        }

    }

    private static final String WORKING_DIR =
        FullFrontierReport.class.getSimpleName();

    /** The logger for this class. */
    private static final Log LOG =
        LogFactory.getLog(FullFrontierReport.class);

    /**
     * The Berkeley DB JE environment.
     */
    private final Environment dbEnvironment;

    /**
     * The BDB entity store.
     */
    private final EntityStore store;

    /**
     * Primary index.
     */
    private final PrimaryIndex<PersistentLineKey, PersistentLine> linesIndex;

    /**
     * Secondary index, per domain name.
     */
    private final SecondaryIndex<String, PersistentLineKey, PersistentLine>
        linesByDomain;

    /**
     * Secondary index, per current size.
     */
    private final SecondaryIndex<Long, PersistentLineKey, PersistentLine>
        linesByCurrentSize;

    /**
     * Secondary index, per spent budget.
     */
    private final SecondaryIndex<Long, PersistentLineKey, PersistentLine>
        linesBySpentBudget;

    /**
     * The directory where the BDB is stored.
     */
    private final File storageDir;

    /**
     * Builds an empty frontier report wrapper.
     * @param jobName the Heritrix job name
     */
    private FullFrontierReport(String jobName) {
        super(jobName);

        File workingDir = new File(
                Settings.getFile(CommonSettings.CACHE_DIR),
                WORKING_DIR);

        this.storageDir = new File(workingDir, jobName);
        if (!storageDir.mkdirs()) {
            throw new IOFailure("Failed to create directory "
                    + storageDir.getAbsolutePath());
        }

        try {
            EnvironmentConfig envConfig = new EnvironmentConfig();
            envConfig.setAllowCreate(true);
            dbEnvironment = new Environment(storageDir, envConfig);

            StoreConfig storeConfig = new StoreConfig();
            storeConfig.setAllowCreate(true);

            store = new EntityStore(
                    dbEnvironment,
                    FrontierReportLine.class.getSimpleName() + "-" + jobName,
                    storeConfig);

            linesIndex = store.getPrimaryIndex(
                    PersistentLineKey.class, PersistentLine.class);

            linesByDomain = store.getSecondaryIndex(
                    linesIndex, String.class, "domainNameKey");

            linesByCurrentSize = store.getSecondaryIndex(
                    linesIndex, Long.class, "currentSizeKey");

            linesBySpentBudget = store.getSecondaryIndex(
                    linesIndex, Long.class, "totalSpendKey");

        } catch (DatabaseException e) {
            throw new IOFailure(
                    "Failed to init frontier BDB for job " + jobName, e);
        }

    }

    /**
     * Releases all resources once this report is to be discarded.
     * NB this method MUST be explicitly called!
     */
    public void dispose() {

        try {
            store.close();
            dbEnvironment.cleanLog();
            dbEnvironment.close();
        } catch (DatabaseException e) {
            throw new IOFailure(
                    "Failed to close frontier BDB for job " + getJobName(), e);
        }

        FileUtils.removeRecursively(storageDir);
    }

    @Override
    public void addLine(FrontierReportLine line) {
        try {
            linesIndex.put(new PersistentLine(line));
        } catch (DatabaseException e) {
            throw new IOFailure(
                    "Failed to store frontier report line for job "
                    + getJobName(), e);
        }
    }

    @Override
    public FrontierReportLine getLineForDomain(String domainName) {
        try {
            return linesByDomain.get(domainName);
        } catch (DatabaseException e) {
            LOG.warn("Failed to get queue for domain " + domainName, e);
            return null;
        }
    }

    /**
     * Returns an iterator where lines are ordered by primary key order:
     * first by decreasing totalEnqueues, then by domain name natural order.
     * @return an iterator on the report lines.
     */
    public ReportIterator iterateOnTotalEnqueues() {
        try {
            return new ReportIterator(linesIndex.entities());
        } catch (DatabaseException e) {
            throw new IOFailure(
                    "Failed to read frontier BDB for job " + getJobName(), e);
        }
    }

    /**
     * Returns an iterator where lines are ordered by domain name natural order.
     * @return an iterator on the report lines.
     */
    public ReportIterator iterateOnDomainName() {
        try {
            return new ReportIterator(linesByDomain.entities());
        } catch (DatabaseException e) {
            throw new IOFailure(
                    "Failed to read frontier BDB for job " + getJobName(), e);
        }
    }

    /**
     * Returns an iterator where lines are ordered by increasing currentSize.
     * @return an iterator on the report lines.
     */
    public ReportIterator iterateOnCurrentSize() {
        try {
            return new ReportIterator(linesByCurrentSize.entities());
        } catch (DatabaseException e) {
            throw new IOFailure(
                    "Failed to read frontier BDB for job " + getJobName(), e);
        }
    }

    /**
     * Returns an iterator on lines having a given currentSize.
     * @param dupValue 
     * @return an iterator on the report lines.
     */
    public ReportIterator iterateOnDuplicateCurrentSize(long dupValue) {
        try {
            return new ReportIterator(
                    linesByCurrentSize.subIndex(dupValue).entities());
        } catch (DatabaseException e) {
            throw new IOFailure(
                    "Failed to read frontier BDB for job " + getJobName(), e);
        }
    }

    /**
     * Returns an iterator where lines are ordered by increasing totalSpend.
     * @return an iterator on the report lines.
     */
    public ReportIterator iterateOnSpentBudget() {
        try {
            return new ReportIterator(linesBySpentBudget.entities());
        } catch (DatabaseException e) {
            throw new IOFailure(
                    "Failed to read frontier BDB for job " + getJobName(), e);
        }
    }

    /**
     * Returns an iterator on lines having a given totalSpend.
     * @param dupValue 
     * @return an iterator on the report lines.
     */
    public ReportIterator iterateOnDuplicateSpentBudget(long dupValue) {
        try {
            return new ReportIterator(
                    linesBySpentBudget.subIndex(dupValue).entities());
        } catch (DatabaseException e) {
            throw new IOFailure(
                    "Failed to read frontier BDB for job " + getJobName(), e);
        }
    }

    /**
     * Generates an Heritrix frontier report wrapper object by parsing
     * the frontier report returned by the JMX controller as a string.
     * @param jobName the Heritrix job name
     * @param contentsAsString the text returned by the JMX call
     * @return the report wrapper object
     */
    public static FullFrontierReport parseContentsAsString(
            String jobName,
            String contentsAsString)  {

        FullFrontierReport report = new FullFrontierReport(jobName);

        // First dump this possibly huge string to a file
        File tmpDir = Settings.getFile(CommonSettings.CACHE_DIR);
        File tmpFile = new File(
                tmpDir,
                jobName + "-" + System.currentTimeMillis() + ".txt");
        try {
            tmpFile.createNewFile();
            BufferedWriter out = new BufferedWriter(new FileWriter(tmpFile));
            out.write(contentsAsString);
            out.close();
        } catch (IOException e) {
            LOG.error("Failed to create temporary file", e);
            return report;
        }

        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(tmpFile));
        } catch (FileNotFoundException e) {
            LOG.error("Failed to read temporary file", e);
            return report;
        }

        try {
            String lineToken = br.readLine(); // Discard header line
            while ((lineToken = br.readLine()) != null) {
                report.addLine(new FrontierReportLine(lineToken));
            }

            br.close();
        } catch (IOException e) {
            LOG.warn("Failed to close reader", e);
        } catch (Throwable t) {
            LOG.error(t);
            t.printStackTrace(System.err);
        } finally {
            FileUtils.remove(tmpFile);
        }

        return report;
    }

    /**
     * Return the directory where the BDB is stored.
     * @return the storage directory.
     */
    File getStorageDir() {
        return storageDir;
    }

}
