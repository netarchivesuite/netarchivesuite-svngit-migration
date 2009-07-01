package dk.netarkivet.wayback;

/* $ ID: TestInfo.java Jul 1, 2009 1:04:32 PM hbk $
* $ Revision: $
* $ Date: Jul 1, 2009 1:04:32 PM $ 
* $ @auther hbk $
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

import java.io.File;

/**
 * Defines test data and directories for the package
 * dk.netarkivet.archive.arcrepository.
 */
class TestInfo {
    static final File DATA_DIR
        = new File("tests/dk/netarkivet/wayback/data/");
    static final File ORIGINALS_DIR = new File(DATA_DIR, "originals");
    static final File WORKING_DIR = new File(DATA_DIR, "working");
    static final File FILE_DIR = new File(WORKING_DIR, "filedir");
    /**static final File CORRECT_ORIGINALS_DIR = new File(DATA_DIR,
            "correct/originals/");
    static final File CORRECT_WORKING_DIR = new File(DATA_DIR,
            "correct/working/");
    static final File TMP_FILE = new File(WORKING_DIR, "temp");*/
    static final File LOG_FILE = new File("tests/testlogs/netarkivtest.log");
    public static final long SHORT_TIMEOUT = 1000;
}
