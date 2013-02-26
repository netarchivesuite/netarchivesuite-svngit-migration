/*$Id$
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
package dk.netarkivet.archive.arcrepository;

import java.io.ByteArrayOutputStream;
import java.io.File;

import junit.framework.TestCase;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.arcrepository.TrivialArcRepositoryClient;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClient;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.utils.ChecksumCalculator;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.batch.FileListJob;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;

/**
 * Unit-tests for the class 
 * TrivialArcRepositoryClient
 */
public class TrivialArcRepositoryClientTester extends TestCase {
    MoveTestFiles mtf = new MoveTestFiles(dk.netarkivet.archive.distribute.arcrepository.TestInfo.ORIGINALS_DIR,
            dk.netarkivet.archive.distribute.arcrepository.TestInfo.WORKING_DIR);
    ReloadSettings rs = new ReloadSettings();
    UseTestRemoteFile utrf = new UseTestRemoteFile();

    public TrivialArcRepositoryClientTester(String s) {
        super(s);
    }

    public void setUp() {
        rs.setUp();
        utrf.setUp();

        Settings.set(CommonSettings.DIR_COMMONTEMPDIR, dk.netarkivet.archive.distribute.arcrepository.TestInfo.WORKING_DIR.getAbsolutePath());
        mtf.setUp();
    }

    public void tearDown() {
        mtf.tearDown();
        utrf.tearDown();
        rs.tearDown();
    }

    public void testStore() throws Exception {
        ArcRepositoryClient arcrep = new TrivialArcRepositoryClient();

        BatchStatus status = arcrep.batch(new FileListJob(), "BA");
        assertEquals("Should have no files processed at outset",
                0, status.getNoOfFilesProcessed());

        FileUtils.copyFile(dk.netarkivet.archive.distribute.arcrepository.TestInfo.SAMPLE_FILE, dk.netarkivet.archive.distribute.arcrepository.TestInfo.SAMPLE_FILE_COPY);
        arcrep.store(dk.netarkivet.archive.distribute.arcrepository.TestInfo.SAMPLE_FILE);
        assertFalse("Should have deleted file after upload",
                dk.netarkivet.archive.distribute.arcrepository.TestInfo.SAMPLE_FILE.exists());
        status = arcrep.batch(new FileListJob(), "BA");
        assertEquals("Should have 1 files processed after store",
                1, status.getNoOfFilesProcessed());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        status.getResultFile().appendTo(out);
        assertEquals("Should list the one file",
                dk.netarkivet.archive.distribute.arcrepository.TestInfo.SAMPLE_FILE.getName() + "\n",
                out.toString());
        File f = File.createTempFile("foo", "bar", FileUtils.getTempDir());
        arcrep.getFile(dk.netarkivet.archive.distribute.arcrepository.TestInfo.SAMPLE_FILE.getName(), Replica.getReplicaFromId("TWO"), f);
        assertEquals("Should have expected contents back",
                ChecksumCalculator.calculateMd5(dk.netarkivet.archive.distribute.arcrepository.TestInfo.SAMPLE_FILE_COPY),
                ChecksumCalculator.calculateMd5(f));
    }
}