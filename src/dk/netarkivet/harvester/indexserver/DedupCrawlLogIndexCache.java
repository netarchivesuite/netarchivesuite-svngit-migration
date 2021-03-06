/* File:     $Id$
 * Revision: $Revision$
 * Author:   $Author$
 * Date:     $Date$
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
 *  USA
 */

package dk.netarkivet.harvester.indexserver;

import java.util.Set;

import dk.netarkivet.common.exceptions.NotImplementedException;

/**
 * A cache of crawl log indices appropriate for the Icelandic deduplicator
 * code, excluding all text entries.
 *
 * @see CrawlLogIndexCache
 *
 */
public class DedupCrawlLogIndexCache extends CrawlLogIndexCache {
    /**
     * Constructor.
     * Calls the constructor of the inherited class with the correct values.
     */
    public DedupCrawlLogIndexCache() {
        super("dedupcrawllogindex", true, "^text/.*");
    }

    @Override
    public void requestIndex(Set<Long> jobSet, Long harvestId) {
        throw new NotImplementedException(
        "This feature is not implemented for the cdxIndexCache");        
    }
}

