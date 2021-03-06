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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *   USA
 */
package dk.netarkivet.wayback.indexer;

import java.io.File;

import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.wayback.WaybackSettings;

public class ArchiveFileTester extends IndexerTestCase {
    private final File destDir = Settings.getFile(WaybackSettings.WAYBACK_BATCH_OUTPUTDIR);


    @Override
    public void setUp() {
        super.setUp();
        FileUtils.removeRecursively(destDir);
    }

    @Override
    public void tearDown() {
        super.tearDown();
        FileUtils.removeRecursively(destDir);
    }

    /**
     * Test indexing on an archive arcfile
     */
    public void testIndexerArc() {
        ArchiveFile file = new ArchiveFile();
        file.setFilename("arcfile_withredirects.arc");
        (new ArchiveFileDAO()).create(file);
        file.index();
        File outputFile = new File(destDir,
                                   file.getOriginalIndexFileName());
        assertTrue("Should have a resonable numer of lines in output file",
                   FileUtils.countLines(outputFile)>5);
    }

    /**
     * Test indexing on a metadata arcfile
     */
    public void testIndexerMetadata() {
        ArchiveFile file = new ArchiveFile();
        file.setFilename("duplicate.metadata.arc");
        (new ArchiveFileDAO()).create(file);
        file.index();
        File outputFile = new File(destDir,
                                   file.getOriginalIndexFileName());
        assertTrue("Should have a resonable numer of lines in output file",
                   FileUtils.countLines(outputFile) == 15);
    }


}
