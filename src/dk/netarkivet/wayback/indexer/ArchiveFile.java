/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *   USA
 */
package dk.netarkivet.wayback.indexer;

import javax.persistence.Id;
import javax.persistence.Entity;
import java.util.Date;
import java.util.UUID;
import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClient;
import dk.netarkivet.common.distribute.arcrepository.PreservationArcRepositoryClient;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.wayback.batch.ExtractDeduplicateCDXBatchJob;
import dk.netarkivet.wayback.batch.ExtractWaybackCDXBatchJob;
import dk.netarkivet.wayback.WaybackSettings;


/**
 * This class represents a file in the arcrepository which may be indexed by
 * the indexer.
 */
@Entity
public class ArchiveFile {

    /**
     * Logger for this class.
     */
    private static Log log = LogFactory.getLog(ArchiveFile.class);

    /**
     * The name of the file in the arcrepository.
     */
    private String filename;

    /**
     * Boolean flag indicating whether the file has been indexed.
     */
    private boolean isIndexed;

    /**
     * The name of the unsorted cdx index file created from the archive file.
     */
    private String originalIndexFileName;

    /**
     * The name of the sorted index file to which entries from this file have
     * been added.
     */
    private String finalIndexFileName;

    /**
     * The number of times an attempt to index this file has failed.
     */
    private int indexingFailedAttempts;


    /**
     * The date on which this file was indexed.
     */
    private Date indexedDate;

    public ArchiveFile() {
        isIndexed = false;
        indexedDate = null;
    }

    public String getOriginalIndexFileName() {
        return originalIndexFileName;
    }

    public void setOriginalIndexFileName(String originalIndexFileName) {
        this.originalIndexFileName = originalIndexFileName;
    }

    public String getFinalIndexFileName() {
        return finalIndexFileName;
    }

    public void setFinalIndexFileName(String finalIndexFileName) {
        this.finalIndexFileName = finalIndexFileName;
    }

    public Date getIndexedDate() {
        return indexedDate;
    }

    public void setIndexedDate(Date indexedDate) {
        this.indexedDate = indexedDate;
    }

    /**
     * The filename is used as a natural key because it is a fundamental property
     * of the arcrepository that filenames are unique.
     * @return the filename.
     */
    @Id
    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public boolean isIndexed() {
        return isIndexed;
    }

    public void setIndexed(boolean indexed) {
        isIndexed = indexed;
    }

    public int getIndexingFailedAttempts() {
        return indexingFailedAttempts;
    }

    public void setIndexingFailedAttempts(int indexingFailedAttempts) {
        this.indexingFailedAttempts = indexingFailedAttempts;
    }

    /**
     * Run a batch job to index this file, storing the result locally.
     * If this method runs successfully, the isIndexed flag will be set to
     * true and the originalIndexFileName field will be set to the (arbitrary)
     * name of the file containing the results. The values are persisted to the
     * datastore.
     */
    public void index() throws IllegalState {
        if (isIndexed) {
            throw new IllegalState("Attempted to index file '" + filename +
                                   "' which is already indexed");
        }
        //TODO the following if-block could be replaced by some fancier more general
        //class with methods for associating particular types of archived files
        //with particular types of batch processor. e.g. something with
        // a signature like
        // List<FileBatchJob> getIndexers(ArchiveFile file)
        // This more-flexible approach
        //may be of value when we begin to add warc support.
        FileBatchJob theJob = null;
        if (filename.contains("metadata")) {
            theJob = new ExtractDeduplicateCDXBatchJob();
        } else {
            theJob = new ExtractWaybackCDXBatchJob();
        }
        theJob.processOnlyFileNamed(filename);                
        PreservationArcRepositoryClient client =
                ArcRepositoryClientFactory.getPreservationInstance();
        BatchStatus batchStatus = client.batch(theJob, Settings.get(
                WaybackSettings.WAYBACK_REPLICA));
        if (!batchStatus.getFilesFailed().isEmpty() ||
            batchStatus.getNoOfFilesProcessed() != 1 ||
            !batchStatus.getExceptions().isEmpty()) {
            logBatchError(batchStatus);
        } else {
            collectResults(batchStatus);
        }
    }

    private void collectResults(BatchStatus status) {
        String outputFilename = UUID.randomUUID().toString();
        String tempBatchOutputDir =
                Settings.get(WaybackSettings.WAYBACK_INDEX_TEMPDIR);
        final File outDir = new File(tempBatchOutputDir);
        FileUtils.createDir(outDir);
        File batchOutputFile =
                new File(outDir, outputFilename);
        status.copyResults(batchOutputFile);
        String finalBatchOutputDir =
                Settings.get(WaybackSettings.WAYBACK_BATCH_OUTPUTDIR);
        final File finalDirectory = new File(finalBatchOutputDir);
        FileUtils.createDir(finalDirectory);
        File finalFile =
                new File(finalDirectory, outputFilename);
        batchOutputFile.renameTo(finalFile);
        originalIndexFileName = outputFilename;
        isIndexed = true;
        (new ArchiveFileDAO()).update(this);
    }

    private void logBatchError(BatchStatus status) {
        String message = "Error indexing file '" + getFilename() + "\n" +
                         "Number of files processed: '" +
                         status.getNoOfFilesProcessed() + "\n" +
                         "Number of files failed + '" +
                         status.getFilesFailed().size();
        if (!status.getExceptions().isEmpty()) {
            message += "\n Exceptions thrown: " + "\n";
            for (FileBatchJob.ExceptionOccurrence e: status.getExceptions()) {
                message += e.toString() + "\n";
            }
        }
        log.error(message);
        indexingFailedAttempts += 1;
        (new ArchiveFileDAO()).update(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ArchiveFile that = (ArchiveFile) o;

        if (indexingFailedAttempts != that.indexingFailedAttempts) {
            return false;
        }
        if (isIndexed != that.isIndexed) {
            return false;
        }
        if (!filename.equals(that.filename)) {
            return false;
        }
        if (finalIndexFileName != null ? !finalIndexFileName.equals(
                that.finalIndexFileName) : that.finalIndexFileName != null) {
            return false;
        }
        if (indexedDate != null ? !indexedDate.equals(that.indexedDate)
                                : that.indexedDate != null) {
            return false;
        }
        if (originalIndexFileName != null ? !originalIndexFileName.equals(
                that.originalIndexFileName) : that.originalIndexFileName
                                              != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = filename.hashCode();
        result = 31 * result + (isIndexed ? 1 : 0);
        result = 31 * result + (originalIndexFileName != null
                                ? originalIndexFileName.hashCode() : 0);
        result = 31 * result + (finalIndexFileName != null
                                ? finalIndexFileName.hashCode() : 0);
        result = 31 * result + indexingFailedAttempts;
        result = 31 * result + (indexedDate != null ? indexedDate.hashCode()
                                                    : 0);
        return result;
    }
}
