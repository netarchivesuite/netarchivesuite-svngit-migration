/* File:        $Id$
 * Revision:    $Revision$
 * Date:        $Date$
 * Author:      $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

package dk.netarkivet.archive.bitarchive;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.archive.Constants;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.ApplicationUtils;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;

/**
 * This class handles file lookup and encapsulates the actual placement of
 * files.
 */
public class BitarchiveAdmin {
    /** The class logger. */
    private final Log log = LogFactory.getLog(getClass().getName());

    /**
     * The list of valid archive directories.
     */
    private List<File> archivePaths = new ArrayList<File>();

    /**
     * Singleton instance.
     */
    private static BitarchiveAdmin instance;

    /**
     * How much space we must have available *in a single dir*
     * before we will listen  for new uploads.
     */
    private final long minSpaceLeft;

    /**
     * How much space we require available *in every dir*
     * after we have accepted an upload.
     */
    private final long minSpaceRequired;

    /**
     * Creates a new BitarchiveAdmin object for an existing bit archive.
     * Reads the directories to use from settings.
     *
     * @throws ArgumentNotValid If the settings for minSpaceLeft is non-positive
     *                          or the setting for minSpaceRequired is negative. 
     * @throws PermissionDenied If any of the directories cannot be created or
     *                          are not writeable.
     */
    private BitarchiveAdmin() {
        String[] filedirnames =
                Settings.getAll(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR);
        minSpaceLeft = Settings.getLong(
                ArchiveSettings.BITARCHIVE_MIN_SPACE_LEFT);
        // Check, if value of minSpaceLeft is greater than zero
        if (minSpaceLeft <= 0L) {
            log.warn(
                    "Wrong setting of minSpaceLeft read from Settings: "
                    + minSpaceLeft);
            throw new ArgumentNotValid(
                    "Wrong setting of minSpaceLeft read from Settings: "
                    + minSpaceLeft);
        }

        minSpaceRequired = Settings.getLong(
                ArchiveSettings.BITARCHIVE_MIN_SPACE_REQUIRED);
        // Check, if value of minSpaceRequired is at least zero
        if (minSpaceLeft < 0L) {
            log.warn(
                    "Wrong setting of minSpaceRequired read from Settings: "
                    + minSpaceLeft);
            throw new ArgumentNotValid(
                    "Wrong setting of minSpaceRequired read from Settings: "
                    + minSpaceLeft);
        }

        log.info("Requiring at least " + minSpaceRequired + " bytes free.");
        log.info("Listening if at least " + minSpaceLeft + " bytes free.");

        for (String filedirname : filedirnames) {
            File basedir = new File(filedirname);
            File filedir = new File(basedir, Constants.FILE_DIRECTORY_NAME);

            // Ensure that 'filedir' exists. If it doesn't, it is created
            ApplicationUtils.dirMustExist(filedir);
            File tempdir = new File(basedir,
                                    Constants.TEMPORARY_DIRECTORY_NAME);

            // Ensure that 'tempdir' exists. If it doesn't, it is created
            ApplicationUtils.dirMustExist(tempdir);

            File atticdir = new File(basedir, Constants.ATTIC_DIRECTORY_NAME);

            // Ensure that 'atticdir' exists. If it doesn't, it is created
            ApplicationUtils.dirMustExist(atticdir);
            archivePaths.add(basedir);
            final Long bytesUsedInDir = calculateBytesUsed(basedir);
            log.info("Using bit archive directory '" + basedir + "' with "
                     + bytesUsedInDir + " bytes of content and "
                     + FileUtils.getBytesFree(basedir) + " bytes free");
        }
    }

    /**
     * Returns true if we have at least one dir with the required amount
     * of space left.
     *
     * @return true if we have at least one dir with the required amount
     *         of space left, otherwise false.
     */
    public boolean hasEnoughSpace() {
        for (File dir : archivePaths) {
            if (checkArchiveDir(dir)
                && FileUtils.getBytesFree(dir) > minSpaceLeft) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a temporary place for the the file to be stored.
     *
     * @param arcFileName The simple name (i.e. no dirs) of the ARC file.
     * @param requestedSize How large the file is in bytes.
     * @return The path where the arcFile should go.
     *
     * @throws ArgumentNotValid
     *          If arcFileName is null or empty, or requestedSize is negative.
     * @throws IOFailure if there is no more room left to store this file of
     *                   size=requestedSize
     */
    public File getTemporaryPath(String arcFileName, long requestedSize)
    throws ArgumentNotValid, IOFailure {
        ArgumentNotValid.checkNotNullOrEmpty(arcFileName, "arcFile");
        ArgumentNotValid.checkNotNegative(requestedSize, "requestedSize");

        for (File dir : archivePaths) {
            long bytesFreeInDir = FileUtils.getBytesFree(dir);
            // TODO If it turns out that it has not enough space for
            // this file, it should resend the Upload message
            // This should probably be handled in the
            // method BitarchiveServer.visit(UploadMessage msg)
            // This is bug 1586.

            if (checkArchiveDir(dir)
                && (bytesFreeInDir > minSpaceLeft)
                && (bytesFreeInDir - requestedSize > minSpaceRequired)) {
                File filedir = new File(
                        dir, Constants.TEMPORARY_DIRECTORY_NAME);
                return new File(filedir, arcFileName);
            } else {
                log.debug("Not enough space on dir '"
                          + dir.getAbsolutePath() + "' for file '"
                          + arcFileName + "' of size " + requestedSize
                          + " bytes. Only " + bytesFreeInDir + " left");
            }
        }
        String errMsg = "No space left to store file '" + arcFileName
            + "' of size " + requestedSize;

        log.error(errMsg);
        throw new IOFailure(errMsg);
    }

    /**
     * Moves a file from temporary storage to file storage.
     *
     * Note: It is checked, if tempLocation resides in directory
     *  TEMPORARY_DIRECTORY_NAME and whether the parent of tempLocation
     *   is a Bitarchive directory.
     *
     * @param tempLocation The temporary location where the file was stored.
     *                     This must be a path returned from getTemporaryPath
     *
     * @return The location where the file is now stored
     * @throws IOFailure if tempLocation is not created from getTemporaryPath
     *                   or file cannot be moved to Storage location.
     */
    public File moveToStorage(File tempLocation) {
        ArgumentNotValid.checkNotNull(tempLocation, "tempLocation");
        tempLocation = tempLocation.getAbsoluteFile();
        String arcFileName = tempLocation.getName();

        /**
         * Check, that File tempLocation resides in directory
         * TEMPORARY_DIRECTORY_NAME.
         */
        File arcFilePath = tempLocation.getParentFile();
        if (arcFilePath == null
            || !arcFilePath.getName().equals(
                    Constants.TEMPORARY_DIRECTORY_NAME)) {
            throw new IOFailure("Location '" + tempLocation + "' is not in "
                                + "tempdir '" 
                                + Constants.TEMPORARY_DIRECTORY_NAME + "'");
        }
        /**
         * Check, that arcFilePath (now known to be TEMPORARY_DIRECTORY_NAME)
         * resides in a recognised Bitarchive Directory.
         */
        File archivedir = arcFilePath.getParentFile();
        if (archivedir == null || !isBitarchiveDirectory(archivedir)) {
            throw new IOFailure("Location '" + tempLocation + "' is not in "
                                + "recognised archive directory.");
        }
        /**
         * Move File tempLocation to new location: storageFile
         */
        File storagePath = new File(archivedir, 
                Constants.FILE_DIRECTORY_NAME);
        File storageFile = new File(storagePath, arcFileName);
        if (!tempLocation.renameTo(storageFile)) {
            throw new IOFailure("Could not move '" + tempLocation.getPath()
                                + "' to '" + storageFile.getPath() + "'");
        }
        return storageFile;
    }

    /** 
     * Checks whether a directory is one of the known bitarchive directories.
     *
     * @param theDir The dir to check
     * @return true If it is a valid archive directory; otherwise returns false.
     * @throws IOFailure if theDir or one of the valid archive directories
     * does not exist
     * @throws ArgumentNotValid if theDir is null
     */
    protected boolean isBitarchiveDirectory(File theDir) {
        ArgumentNotValid.checkNotNull(theDir, "File theDir");
        try {
            theDir = theDir.getCanonicalFile();
            for (File knowndir : archivePaths) {
                if (knowndir.getCanonicalFile().equals(theDir)) {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            final String errMsg = "File not known";
            log.warn(errMsg, e);
            throw new IOFailure(errMsg, e);
        }
    }

    /**
     * Check that the given file is a directory appropriate for use.
     * A File is appropiate to use as archivedir, if the file is an
     * existing directory, and is writable by this java process.
     *
     * @param file A file
     * @return true, if 'file' is an existing directory and is writable.
     * @throws ArgumentNotValid if 'file' is null.
     */
    private boolean checkArchiveDir(File file) {
        ArgumentNotValid.checkNotNull(file, "file");
        if (!file.exists()) {
            log.warn("Directory '" + file + "' does not exist");
            return false;
        }
        if (!file.isDirectory()) {
            log.warn("Directory '" + file
                    + "' is not a directory after all");
            return false;
        }
        if (!file.canWrite()) {
            log.warn("Directory '" + file + "' is not writable");
            return false;
        }
        return true;
    }

    /**
     * Return array with references to all files in the archive.
     *
     * @return array with references to all files in the archive
     */
    public File[] getFiles() {
        List<File> files = new ArrayList<File>();
        for (File archivePath : archivePaths) {
            File archiveDir = new File(archivePath,
                                       Constants.FILE_DIRECTORY_NAME);
            if (checkArchiveDir(archiveDir)) {
                File[] filesHere = archiveDir.listFiles();
                for (File file : filesHere) {
                    if (!file.isFile()) {
                        log.warn("Non-file '" + file.getAbsolutePath()
                                 + "' found among archive files");
                    } else {
                        files.add(file);
                    }
                }
            }
        }
        return files.toArray(new File[files.size()]);
    }

    /** Return an array of all files in this archive that match a given
     * regular expression on the filename.
     *
     * @param regexp A precompiled regular expression matching whole filenames.
     * This will probably be given to a FilenameFilter
     * @return An array of all the files in this bitarchive that exactly match
     * the regular expression on the filename (sans paths).
     */
    public File[] getFilesMatching(final Pattern regexp) {
        ArgumentNotValid.checkNotNull(regexp, "Pattern regexp");
        List<File> files = new ArrayList<File>();
        for (File archivePath : archivePaths) {
            File archiveDir = new File(archivePath, 
                    Constants.FILE_DIRECTORY_NAME);
            if (checkArchiveDir(archiveDir)) {
                File[] filesHere = archiveDir.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        if (regexp.matcher(name).matches()) {
                            if (!new File(dir, name).isFile()) {
                                log.warn("Non regular file '" 
                                        + new File(dir, name).getAbsolutePath()
                                        + "' found among archive files");
                                return false;
                            } else {
                                return true;
                            }
                        } else {
                            return false;
                        }
                    }
                });
                files.addAll(Arrays.asList(filesHere));
            }
        }
        return files.toArray(new File[files.size()]);
    }

    /**
     * Return the path that a given arc file can be found in.
     *
     * @param arcFileName Name of an arc file (with no path)
     * @return A BitarchiveARCFile for the given file, or null if the
     *         file does not exist.
     */
    public BitarchiveARCFile lookup(String arcFileName) {
        ArgumentNotValid.checkNotNullOrEmpty(arcFileName, "arcFileName");
        for (File archivePath : archivePaths) {
            File archiveDir = new File(archivePath,
                                       Constants.FILE_DIRECTORY_NAME);

            if (checkArchiveDir(archiveDir)) {
                File filename = new File(archiveDir, arcFileName);
                if (filename.exists()) {
                    if (filename.isFile()) {
                        return new BitarchiveARCFile(arcFileName, filename);
                    }
                    log.fatal("Possibly corrupt bitarchive: Non-file '"
                              + filename + "' found in"
                              + " place of archive file");
                }
            }
        }
        // the arcfile named "arcFileName" does not exist in this bitarchive.
        log.trace("The arcfile named '" + arcFileName 
                + "' does not exist in this bitarchve");
        return null;
    }

    /**
     * Calculate how many bytes are used by all files in a directory.
     *
     * @param filedir An existing directory with a FILE_DIRECTORY_NAME subdir
     *                and a TEMPORARY_DIRECTORY_NAME subdir.
     * @return Number of bytes used by all files in the directory (not including
     *         overhead from partially used blocks).
     */
    private long calculateBytesUsed(File filedir) {
        long used = 0;
        File[] files = new File(filedir, Constants.FILE_DIRECTORY_NAME)
                 .listFiles();
        // Check, that listFiles method returns valid information
        if (files != null) {
            for (File datafiles : files) {
                if (datafiles.isFile()) {
                    // Add size of file f to amount of bytes used.
                    used += datafiles.length(); 
                } else {
                    log.warn("Non-file '" + datafiles.getAbsolutePath() 
                            + "' found in archive");
                }
            }
        } else {
            log.warn("filedir does not contain a directory named: "
                        + Constants.FILE_DIRECTORY_NAME);
        }
        File[] tempfiles = new File(filedir,
                Constants.TEMPORARY_DIRECTORY_NAME).listFiles();
        // Check, that listFiles() method returns valid information
        if (tempfiles != null) { 
            for (File tempfile : tempfiles) {
                if (tempfile.isFile()) {
                    // Add size of file f to amount of bytes used.
                    used += tempfile.length();
                } else {
                    log.warn(
                            "Non-file '" + tempfile.getAbsolutePath() 
                            + "' found in archive");
                }         
            }
        } else {
            log.warn("filedir does not contain a directory named: "
                        + Constants.TEMPORARY_DIRECTORY_NAME);
        }
        File[] atticfiles = new File(filedir, Constants.ATTIC_DIRECTORY_NAME)
                .listFiles();
        // Check, that listFiles() method returns valid information
        if (atticfiles != null) {
            
            for (File atticfile : atticfiles) {
                if (atticfile.isFile()) {
                    // Add size of file tempfiles[i] to amount of bytes used.
                    used += atticfile.length();
                } else {
                    log.warn("Non-file '" + atticfile.getAbsolutePath()
                                + "' found in archive");
                }
            }
        } else {
            log.warn("filedir does not contain a directory named: "
                        + Constants.ATTIC_DIRECTORY_NAME);
        }
        return used;
    }

    /**
     * Get the one and only instance of the bitarchive admin.
     *
     * @return A BitarchiveAdmin object
     */
    public static BitarchiveAdmin getInstance() {
        if (instance == null) {
            instance = new BitarchiveAdmin();
        }
        return instance;
    }

    /**
     * Close down the bitarchive admin.
     * Currently has no data to store.
     */
    public void close() {
        instance = null;
    }

    /** Return the path used to store files that are removed by
     * RemoveAndGetFileMessage.
     *
     * @param existingFile a File object for an existing file in the bitarchive
     * 
     * @return The full path of the file in the attic dir
     */
    public File getAtticPath(File existingFile) {
        ArgumentNotValid.checkNotNull(existingFile, "File existingFile");
        // Find where the file resides so we can use a dir in the same place.
        existingFile = existingFile.getAbsoluteFile();
        String arcFileName = existingFile.getName();
        File parentDir = existingFile.getParentFile().getParentFile();
        if (!isBitarchiveDirectory(parentDir)) {
            log.warn("Attempt to get attic path for non-archived file '"
                    + existingFile + "'");
            throw new ArgumentNotValid("File should belong to a bitarchive dir,"
                    + " but " + existingFile + " doesn't");
        }
        // Ensure that 'atticdir' exists. If it doesn't, it is created
        File atticdir = new File(parentDir, Constants.ATTIC_DIRECTORY_NAME);
        ApplicationUtils.dirMustExist(atticdir);
        return new File(atticdir, arcFileName);
    }
}
