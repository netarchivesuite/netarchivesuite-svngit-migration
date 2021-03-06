/* $Id$
 * $Revision$
 * $Author$
 * $Date$
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
package dk.netarkivet.common.distribute;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;

import junit.framework.TestCase;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.ChecksumCalculator;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;

/** Tests for HTTPRemoteFile */

public class HTTPRemoteFileTester extends TestCase {
    MoveTestFiles mtf = new MoveTestFiles(TestInfo.ORIGINALS_DIR,
                                          TestInfo.WORKING_DIR);
    UseTestRemoteFile utrf = new UseTestRemoteFile();
    ReloadSettings rs = new ReloadSettings();

    public HTTPRemoteFileTester(String s) {
        super(s);
    }

    public void setUp() {
        rs.setUp();
        utrf.setUp();
        mtf.setUp();
        // Make sure we're using the right HTTP remote file by closing old
        // registries.
        HTTPSRemoteFileRegistry.getInstance().cleanup();
        HTTPRemoteFileRegistry.getInstance().cleanup();
    }

    public void tearDown() {
        HTTPSRemoteFileRegistry.getInstance().cleanup();
        HTTPRemoteFileRegistry.getInstance().cleanup();
        mtf.tearDown();
        utrf.tearDown();
        rs.tearDown();
    }

    public void testCopyto() throws Exception {
        //Copying twice with multiple
        HTTPRemoteFile rf = new ForceRemoteHTTPRemoteFile(TestInfo.FILE1,
                                                          false, false, true);
        File tempFile = File
                .createTempFile("TEST", "COPYTO", TestInfo.WORKING_DIR);
        rf.copyTo(tempFile);
        String contents = FileUtils.readFile(TestInfo.FILE1);
        assertEquals("Files should be equal",
                     contents,
                     FileUtils.readFile(tempFile));
        assertTrue("Original file should still exist when not deletable",
                   TestInfo.FILE1.exists());

        File tempFile2 = File
                .createTempFile("TEST", "COPYTO", TestInfo.WORKING_DIR);
        rf.copyTo(tempFile2);
        assertEquals("Files should be equal, since multiple was true",
                     contents,
                     FileUtils.readFile(tempFile));
        assertTrue("Original file should still exist when not deletable",
                   TestInfo.FILE1.exists());

        //Copying twice without multiple
        rf = new ForceRemoteHTTPRemoteFile(TestInfo.FILE1,
                                           false, false, false);
        tempFile = File
                .createTempFile("TEST", "COPYTO", TestInfo.WORKING_DIR);
        rf.copyTo(tempFile);
        assertEquals("Files should be equal",
                     contents,
                     FileUtils.readFile(tempFile));
        assertTrue("Original file should still exist when not deletable",
                   TestInfo.FILE1.exists());

        tempFile2 = File
                .createTempFile("TEST", "COPYTO", TestInfo.WORKING_DIR);
        try {
            rf.copyTo(tempFile2);
            fail("Multiple copies should not be allowed");
        } catch (IOFailure e) {
            //expected
        }

        //Copying with non-multiple and deletable
        rf = new ForceRemoteHTTPRemoteFile(TestInfo.FILE1,
                                           false, true, false);
        tempFile = File
                .createTempFile("TEST", "COPYTO", TestInfo.WORKING_DIR);
        rf.copyTo(tempFile);
        assertEquals("Files should be equal",
                     contents,
                     FileUtils.readFile(tempFile));
        assertFalse("Original file shouldn't exist anymore",
                    TestInfo.FILE1.exists());
        //recreate the file for next test
        tempFile.renameTo(TestInfo.FILE1);

        //Copying to a file with no parent
        rf = new ForceRemoteHTTPRemoteFile(TestInfo.FILE1,
                                           false, true, false);
        tempFile = new File("dummyfile");
        tempFile.deleteOnExit();
        rf.copyTo(tempFile);
        assertEquals("Files should be equal",
                     contents,
                     FileUtils.readFile(tempFile));
        assertFalse("Original file shouldn't exist anymore",
                    TestInfo.FILE1.exists());
        FileUtils.remove(tempFile);
    }

    public void testCleanup() throws Exception {
        HTTPRemoteFile rf = new ForceRemoteHTTPRemoteFile(TestInfo.FILE1,
                                                          false, false, true);
        URL url = rf.url;
        url.openConnection().getInputStream();
        rf.cleanup();
        assertTrue("File should still exist when not deletable",
                   TestInfo.FILE1.exists());
        try {
            url.openConnection().getInputStream();
            fail("Should not be available any longer");
        } catch (FileNotFoundException e) {
            //expected
        }

        rf = new ForceRemoteHTTPRemoteFile(TestInfo.FILE1,
                                           false, true, true);
        url = rf.url;
        url.openConnection().getInputStream();
        rf.cleanup();
        assertFalse("File should not exist when deletable",
                    TestInfo.FILE1.exists());
        try {
            url.openConnection().getInputStream();
            fail("Should not be available any longer");
        } catch (FileNotFoundException e) {
            //expected
        }
        //should not throw exception
        rf.cleanup();
    }

    public void testGetChecksum() throws Exception {
        HTTPRemoteFile rf = new ForceRemoteHTTPRemoteFile(TestInfo.FILE1,
                                                          false, false, true);
        assertEquals("Should get null (no checksum requested)",
                     null, rf.getChecksum());
        rf = new ForceRemoteHTTPRemoteFile(TestInfo.FILE1,
                                           true, false, true);
        assertEquals("Should get right checksum",
                     ChecksumCalculator.calculateMd5(TestInfo.FILE1), rf.getChecksum());
    }

    private class ForceRemoteHTTPRemoteFile extends HTTPRemoteFile {
        public ForceRemoteHTTPRemoteFile(File f, boolean useChecksums,
                                         boolean fileDeletable,
                                         boolean multipleDownloads) {
            super(f, useChecksums, fileDeletable, multipleDownloads);
        }

        protected boolean isLocal() {
            return false;
        }
    }
}