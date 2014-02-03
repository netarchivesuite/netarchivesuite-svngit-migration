/* File:       $Id$
 * Revision:   $Revision$
 * Author:     $Author$
 * Date:       $Date$
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

import java.io.Serializable;

import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.harvester.distribute.HarvesterChannels;
import dk.netarkivet.harvester.distribute.HarvesterMessage;
import dk.netarkivet.harvester.distribute.HarvesterMessageVisitor;
import dk.netarkivet.harvester.scheduler.JobDispatcher;

/**
 * The {@link HarvestControllerServer} periodically sends 
 * {@link HarvesterReadyMessage}s to the {@link JobDispatcher} to notify
 * it whether it is available for processing a job or already processing one.
 */
public class HarvesterReadyMessage
        extends HarvesterMessage
        implements Serializable {

    /**
     * The name of the channel of jobs crawled by the sender.
     */
    private final String harvestChannelName;

    /**
     * The sender's application instance ID.
     */
    private final String applicationInstanceId;


    /**
     * Builds a new message.
     * @param harvestChannelName the channel of jobs crawled by the sender.
     * @param applicationInstanceId the sender's application instance ID.
     */
    public HarvesterReadyMessage(
            String applicationInstanceId,
            String harvestChannelName) {
        super(HarvesterChannels.getHarvesterStatusChannel(), Channels.getError());
        this.applicationInstanceId = applicationInstanceId;
        this.harvestChannelName = harvestChannelName;
    }

    @Override
    public void accept(HarvesterMessageVisitor v) {
        v.visit(this);
    }

    /**
     * @return the associated harvest channel name
     */
    public String getHarvestChannelName() {
        return harvestChannelName;
    }

    /**
     * @return the application instance ID.
     */
    public String getApplicationInstanceId() {
        return applicationInstanceId;
    }
}
