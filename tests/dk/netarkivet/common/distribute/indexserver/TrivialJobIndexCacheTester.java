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
package dk.netarkivet.common.distribute.indexserver;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * Unit tests for the TrivialJobIndexCache class. 
 */
public class TrivialJobIndexCacheTester extends TestCase {
    MoveTestFiles mtf = new MoveTestFiles(TestInfo.ORIGINALS_DIR,
            TestInfo.WORKING_DIR);
    ReloadSettings rs = new ReloadSettings();

    public TrivialJobIndexCacheTester(String s) {
        super(s);
    }

    public void setUp() {
        rs.setUp();
        Settings.set(CommonSettings.CACHE_DIR, TestInfo.WORKING_DIR.getAbsolutePath());
        mtf.setUp();
    }

    public void tearDown() {
        mtf.tearDown();
        rs.tearDown();
    }
    public void testCacheData() throws Exception {
        JobIndexCache cache = new TrivialJobIndexCache(RequestType.DEDUP_CRAWL_LOG);
        try {
            cache.getIndex(Collections.singleton(1L)).getIndexFile().getName();
            fail("Expected IOFailure on non-existing cache file");                    
        } catch (IOFailure e) {
            //expected
        }

        Set<Long> jobs = new HashSet<Long>();
        jobs.add(2L);
        jobs.add(3L);
        assertEquals("Should give the expected cache with the right jobs",
                "2-3-DEDUP_CRAWL_LOG-cache", cache.getIndex(jobs).getIndexFile().getName());

    }
}