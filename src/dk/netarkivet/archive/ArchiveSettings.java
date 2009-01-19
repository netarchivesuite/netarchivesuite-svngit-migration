/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.netarkivet.archive;

import dk.netarkivet.common.utils.Settings;

/**
 * Settings specific to the archive module of NetarchiveSuite.
 */
public class ArchiveSettings {
    /** The default place in classpath where the settings file can be found. */
    private static final String DEFAULT_SETTINGS_CLASSPATH
            = "dk/netarkivet/archive/settings.xml";

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
     * Absolute/relative path to where the "central list of files and
     * checksums" (admin.data) is written. Used by ArcRepository and
     * BitPreservation.
     */
    public static String DIRS_ARCREPOSITORY_ADMIN
            = "settings.archive.arcrepository.baseDir";
    /**
     * The minimum amount of bytes left *in any dir* that we will allow a
     * bitarchive machine to accept uploads with.  When no dir has more space
     * than this, the bitarchive machine stops listening for uploads.  This
     * value should at the very least be greater than the largest ARC file
     * you expect to receive.
     */
    public static String BITARCHIVE_MIN_SPACE_LEFT
            = "settings.archive.bitarchive.minSpaceLeft";
    /**
     * These are the directories where ARC files are stored (in a subdir).
     * If more than one is given, they are used from one end.
     * This setting may be repeated, to define multiple directories. 
     */
    public static String BITARCHIVE_SERVER_FILEDIR
            = "settings.archive.bitarchive.fileDir";
    /**
     * The frequency in milliseconds of heartbeats that are sent by each
     * BitarchiveServer to the BitarchiveMonitor.
     */
    public static String BITARCHIVE_HEARTBEAT_FREQUENCY
            = "settings.archive.bitarchive.heartbeatFrequency";
    /**
     * If we haven't heard from a bit archive within this many milliseconds,
     * we don't expect it to be online and won't wait for them to reply on a
     * batch job.  This number should be significantly greater than
     * heartbeatFrequency to account for temporary network congestion.
     */
    public static String BITARCHIVE_ACCEPTABLE_HEARTBEAT_DELAY
            = "settings.archive.bitarchive.acceptableHeartbeatDelay";
    /**
     * The BitarchiveMonitorServer will listen for BatchEndedMessages for this
     * many milliseconds before it decides that a batch job is taking too long
     * and returns just the replies it has received at that point.
     */
    public static String BITARCHIVE_BATCH_JOB_TIMEOUT
            = "settings.archive.bitarchive.batchMessageTimeout";
    /**
     * Credentials to enter in the GUI for "deleting" ARC files in
     * this bit archive.
     */
    public static String ENVIRONMENT_THIS_CREDENTIALS
            = "settings.archive.bitarchive.thisCredentials";

    /**
     * Absolute or relative path to dir containing results of
     * file-list-batch-jobs and checksumming batch jobs for bit preservation.
     */
    public static String DIR_ARCREPOSITORY_BITPRESERVATION
            = "settings.archive.bitpreservation.baseDir";
}
