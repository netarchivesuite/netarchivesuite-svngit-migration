/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
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
package dk.netarkivet.harvester.harvesting.distribute;

import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage.CrawlStatus;
import junit.framework.TestCase;

/**
 * Unit tests for the class
 * {@link CrawlProgressMessage}.
 */
public class CrawlProgressMessageTester extends TestCase {
    
    protected void setUp() throws Exception {
    }
    
    public void testConstructor() {
        long harvestId = 2L;
        long jobId = 42L;
        CrawlProgressMessage msg = new CrawlProgressMessage(harvestId, jobId);
        assertEquals(harvestId, msg.getHarvestID());
        assertEquals(jobId, msg.getJobID());
        assertEquals(CrawlStatus.PRE_CRAWL, msg.getStatus());
        assertEquals("", msg.getProgressStatisticsLegend());
    }
}
