/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

package dk.netarkivet.archive.indexserver;

import java.util.regex.Pattern;

import dk.netarkivet.harvester.harvesting.MetadataFile;

/**
 * A RawDataCache that serves files with CDX data.
 *
 */
public class CDXDataCache extends RawMetadataCache {
    /**
     * Create a new CDXDataCache.  For a given job ID, this will fetch
     * and cache cdx data from metadata files
     * (&lt;ID&gt;-metadata-[0-9]+.arc).
     */
    public CDXDataCache() {
        super("cdxdata",
                Pattern.compile(MetadataFile.CDX_PATTERN),
                Pattern.compile("application/x-cdx"));
    }
}
