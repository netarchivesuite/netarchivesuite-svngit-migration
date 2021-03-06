/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

package dk.netarkivet.harvester.webinterface;

import dk.netarkivet.common.webinterface.SiteSection;
import dk.netarkivet.harvester.datamodel.RunningJobsInfoDAO;

/**
 * Site section that creates the menu for harvest history.
 */

public class HistorySiteSection extends SiteSection {
    /**
     * Create a new history SiteSection object.
     */
    public HistorySiteSection() {
        super("sitesection;history", "Harveststatus", 3,
              new String[][]{
                      {"alljobs", "pagetitle;all.jobs"},
                      {"deprecatedperdomain", "pagetitle;all.jobs.per.domain"},
                      {"running", "pagetitle;all.jobs.running"},
                      {"running-jobdetails", "pagetitle;running.job.details"},
                      {"perhd", "pagetitle;all.jobs.per.harvestdefinition"},
                      {"perharvestrun", "pagetitle;all.jobs.per.harvestrun"},
                      {"jobdetails", "pagetitle;details.for.job"},
                      {"perdomain", "pagetitle;all.jobs.per.domain"},
                      {"seeds", "pagetitle;seeds.for.harvestdefinition" }
              }, "History",
                 dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
    }

    /**
     * No initialisation necessary in this site section.
     */
    public void initialize() {
        // Initialize the running jobs tables if necessary
        RunningJobsInfoDAO.getInstance();
    }

    /** No cleanup necessary in this site section. */
    public void close() {
    }
}
