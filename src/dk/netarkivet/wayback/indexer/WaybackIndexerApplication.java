/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package dk.netarkivet.wayback.indexer;

import dk.netarkivet.common.utils.ApplicationUtils;

/**
 * The entry point for the wayback indexer. This application determines what
 * files in the arcrepository remain to be indexed and indexes them concurrently
 * via batch jobs. The status of all files in the archive is maintained in a
 * persistent object store managed by Hibernate.
 */
public class WaybackIndexerApplication {

    /**
     * Runs the WaybackIndexer. Settings are read from config files so the
     * arguments array should be empty.
     * @param args an empty array.
     */
    public static void main(String[] args) {
        ApplicationUtils.startApp(WaybackIndexer.class, args);
    }

}
