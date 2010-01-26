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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.netarkivet.common.utils.batch;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;

import junit.framework.TestCase;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.arc.TestInfo;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.common.utils.batch.LoadableJarBatchJob;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;

/**
 * Tests for the LoadableJarBatchJob class.
 *
 */
public class LoadableJarBatchJobTester extends TestCase {
    MoveTestFiles mtf = new MoveTestFiles(TestInfo.ORIGINALS_DIR,
                                          TestInfo.WORKING_DIR);
    public LoadableJarBatchJobTester(String s) {
        super(s);
    }

    public void setUp() throws Exception {
        super.setUp();
        mtf.setUp();
    }

    public void tearDown() throws Exception {
        mtf.tearDown();
        super.tearDown();
    }

    public void testInitialize() {
        FileBatchJob job = new LoadableJarBatchJob(
        		"dk.netarkivet.common.utils.batch.LoadableTestJob",
                new File(TestInfo.WORKING_DIR, "LoadableTestJob.jar")
                );
        OutputStream os = new ByteArrayOutputStream();
        job.initialize(os);
        assertEquals("Should have message from loaded class",
                     "initialize() called on me\n", os.toString());

        try {
            job = new LoadableJarBatchJob(
            		"dk.netarkivet.common.utils.batch.LoadableTestJob$InnerClass",
                new File(TestInfo.WORKING_DIR, "LoadableTestJob.jar")
                );
            job.initialize(os);
            fail("Should not be possible to load non-batchjob class");
        } catch (IOFailure e) {
            // Expected
        }

        job = new LoadableJarBatchJob(
        		"dk.netarkivet.common.utils.batch.LoadableTestJob$InnerBatchJob",
        		new File(TestInfo.WORKING_DIR, "LoadableTestJob.jar")
                );
        os = new ByteArrayOutputStream();
        job.initialize(os);
        assertEquals("Should have message from loaded class",
                     "initialize() called on inner\n", os.toString());
    }
    
    public void testLoadingJobWithoutPackage() {
        FileBatchJob job = new LoadableJarBatchJob(
        		"ExternalBatchSeveralClassesNoPackage",
                new File(TestInfo.WORKING_DIR, "ExternalBatchSeveralClassesNoPackage.jar")
                );
        OutputStream os = new ByteArrayOutputStream();
        job.initialize(os);
        
        File metadataFile = new File(TestInfo.WORKING_DIR, "2-metadata-1.arc");
        job.processFile(metadataFile, os);
    }
    public void testLoadingJobWithPackage() {
        FileBatchJob job = new LoadableJarBatchJob(
        		"batch.ExternalBatchSeveralClassesWithPackage",
                new File(TestInfo.WORKING_DIR, "ExternalBatchSeveralClassesWithPackage.jar")
                );
        OutputStream os = new ByteArrayOutputStream();
        job.initialize(os);
        
        File metadataFile = new File(TestInfo.WORKING_DIR, "2-metadata-1.arc");
        job.processFile(metadataFile, os);
    }
    
    
}