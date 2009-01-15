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
package dk.netarkivet.archive.arcrepository.bitpreservation;

import java.util.Date;
import java.util.Map;

import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;

/**
 * All bitpreservation implementations are assumed to have access to admin data
 * and bitarchives. Operations may request information from the bitarchive by
 * sending batch jobs, reading admin data directly, or reading from cached
 * information from either.
 */
public interface ActiveBitPreservation {
    // General state

    /**
     * Get details of the state of one or more files in the bitarchives
     * and admin data.
     * @param filenames the list of filenames to investigate 
     * @return a map ([filename]-> [FilePreservationState]) with the
     *  preservationstate of all files in the list.
     *  The preservationstates in the map will be null for all filenames,
     *  that are not found in admin data.
     */
    Map<String,FilePreservationState> getFilePreservationStateMap(
            String... filenames);

    /**
     * Get the details of the state of the given file in the bitarchives
     * and admin data.
     * @param filename A given file
     * @return the FilePreservationState for the given file. This will be null,
     * if the filename is not found in admin data.
     */
    FilePreservationState getFilePreservationState(String filename);
    
    // Check state for bitarchives

    /**
     * Return a list of files marked as missing on this replica.
     * A file is considered missing if it exists in admin data, but is not known
     * in the bit archives. Guaranteed not to recheck the archive, simply
     * returns the list generated by the last test.
     *
     * @param replica The replica to get missing files from.
     *
     * @return A list of missing files.
     *
     * @throws IOFailure if the list cannot be generated.
     */
    Iterable<String> getMissingFiles(Replica replica);

    /**
     * Return a list of files with changed checksums on this replica.
     * A file is considered changed if checksum does not compare to
     * admin data. Guaranteed not to recheck the archive, simply returns the
     * list generated by the last test.
     *
     * @param replica The replica to get a list of changed files from.
     *
     * @return A list of files with changed checksums.
     *
     * @throws IOFailure if the list cannot be generated.
     */
    Iterable<String> getChangedFiles(Replica replica);

    /** Update the list of files in a given bitarchive. This will be used for
     * the next call to getMissingFiles.
     *
     * @param replica The replica to update list of files for.
     */
    void findMissingFiles(Replica replica);

    /** Update the list of checksums in a given replica. This will be used
     * for the next call to getChangedFiles.
     *
     * @param replica The replica to update list of files for.
     */
    void findChangedFiles(Replica replica);

    /**
     * Return the number of missing files for replica. Guaranteed not to
     * recheck the archive, simply returns the number generated by the last
     * test.
     *
     * @param replica The replica to get the number of missing files from.
     *
     * @return The number of missing files.
     */
    long getNumberOfMissingFiles(Replica replica);

    /**
     * Return the number of changed files for replica. Guaranteed not to
     * recheck the archive, simply returns the number generated by the last
     * test.
     *
     * @param replica The replica to get the number of changed files from.
     *
     * @return The number of changed files.
     */
    long getNumberOfChangedFiles(Replica replica);

    /**
     * Return the total number of files for replica. Guaranteed not to
     * recheck the archive, simply returns the number generated by the last
     * update.
     *
     * @param replica The replica to get the number of files from.
     *
     * @return The number of files.
     */
    long getNumberOfFiles(Replica replica);

    /**
     * Return the date for last check of missing files for replica. Guaranteed
     * not to recheck the archive, simply returns the date for the
     * last test.
     *
     * @param replica The replica to get date for changed files from.
     *
     * @return The date for last check of missing files.
     */
    Date getDateForMissingFiles(Replica replica);

    /**
     * Return the date for last check of changed files for replica. Guaranteed
     * not to recheck the archive, simply returns the date for the
     * last test.
     *
     * @param replica The replica to get date for changed files from.
     *
     * @return The date for last check of changed files.
     */
    Date getDateForChangedFiles(Replica replica);

    // Update files in bitarchives

    /**
     * Check that files are indeed missing on the given replica, and present
     * in admin data and reference replica. If so, upload missing files from
     * reference replica to this replica.
     *
     * @param replica The replica to restore files to
     * @param filenames The names of the files.
     *
     * @throws IOFailure if any file cannot be reestablished, or if any file is
     * not in correct state. The process will be attempted for all files, even
     * if one causes this exception.
     */
    void uploadMissingFiles(Replica replica, String... filenames);

    /**
     * Check that the checksum of the file is indeed different to the value in
     * admin data and reference replica. If so, remove missing file and upload
     * it from reference replica to this replica.
     *
     * @param replica The replica to restore file to
     * @param filename The name of the file
     * @param credentials The credentials used to perform this replace operation
     * @param checksum  The known bad checksum. Only a file with this bad
     * checksum is attempted repaired.
     * @throws IOFailure if the file cannot be reestablished
     * @throws PermissionDenied if the file is not in correct state
     */
    void replaceChangedFile(Replica replica, String filename,
                            String credentials, String checksum);

    // Check state for admin data

    /**
     * Return a list of files represented in replica but missing in AdminData.
     *
     * @return A list of missing files.
     *
     * @throws IOFailure if the list cannot be generated.
     */
    Iterable<String> getMissingFilesForAdminData();

    /**
     * Return a list of files with wrong checksum or state in admin data.
     *
     * @return A list of files with wrong checksum or state.
     *
     * @throws IOFailure if the list cannot be generated.
     */
    Iterable<String> getChangedFilesForAdminData();

    // Update admin data

    /**
     * Add files unknown in admin.data to admin.data.
     *
     * @param filenames The files to add.
     *
     * @throws PermissionDenied if the file is not in correct state
     */
    void addMissingFilesToAdminData(String... filenames);

    /**
     * Reestablish admin data to match bitarchive states for file.
     *
     * @param filename The file to reestablish state for.
     *
     * @throws PermissionDenied if the file is not in correct state
     */
    void changeStateForAdminData(String filename);
}
