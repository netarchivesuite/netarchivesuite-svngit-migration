/* File:    $Id$
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
 * Foundation,dk.netarkivet.harvester.schedulerFloor, Boston, MA  02110-1301  USA
 */
package dk.netarkivet.harvester.scheduler;

import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.lifecycle.ComponentLifeCycle;
import dk.netarkivet.harvester.datamodel.HarvestChannel;
import dk.netarkivet.harvester.datamodel.HarvestChannelDAO;
import dk.netarkivet.harvester.distribute.HarvesterChannels;
import dk.netarkivet.harvester.distribute.HarvesterMessageHandler;
import dk.netarkivet.harvester.harvesting.distribute.HarvesterRegistrationRequest;
import dk.netarkivet.harvester.harvesting.distribute.HarvesterRegistrationResponse;
import dk.netarkivet.harvester.harvesting.distribute.HarvesterReadyMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Handles the reception of status messages from the harvesters. Will call the 
 * {@link #visit(HarvesterReadyMessage)} method when a Ready message is 
 * received.
 */
public class HarvesterStatusReceiver extends HarvesterMessageHandler
        implements ComponentLifeCycle {
    /** @see HarvesterStatusReceiver#visit(dk.netarkivet.harvester.harvesting.distribute.HarvesterReadyMessage)  */
    private final JobDispatcher jobDispatcher;
    /** Connection to JMS provider. */
    private final JMSConnection jmsConnection;
    /** The logger to use.    */
    private final Log log = LogFactory.getLog(getClass());

    /**
     * The DAO handling {@link HarvestChannel}s
     */
    private final HarvestChannelDAO harvestChannelDao;

    private final HarvestChannelRegistry harvestChannelRegistry;

    /**
     * @param jobDispatcher The <code>JobDispatcher</code> to delegate the 
     * dispatching of new jobs to, when a 'Ready for job' event is received.
     * @param jmsConnection The JMS connection by which 
     * {@link HarvesterReadyMessage} is received.
     */
    public HarvesterStatusReceiver(
            JobDispatcher jobDispatcher,
            JMSConnection jmsConnection,
            HarvestChannelDAO harvestChannelDao,
            HarvestChannelRegistry harvestChannelRegistry) {
        ArgumentNotValid.checkNotNull(jobDispatcher, "jobDispatcher");
        ArgumentNotValid.checkNotNull(jmsConnection, "jmsConnection");
        ArgumentNotValid.checkNotNull(harvestChannelDao, "harvestChannelDao");
        this.jobDispatcher = jobDispatcher;
        this.jmsConnection = jmsConnection;
        this.harvestChannelDao = harvestChannelDao;
        this.harvestChannelRegistry = harvestChannelRegistry;
    }

    @Override
    public void start() {
        jmsConnection.setListener(
                HarvesterChannels.getHarvesterStatusChannel(), this);
        jmsConnection.setListener(
                HarvesterChannels.getHarvesterRegistrationRequestChannel(), this);
    }

    @Override
    public void shutdown() {
        jmsConnection.removeListener(
                HarvesterChannels.getHarvesterStatusChannel(), this);
    }

    /**
     * Tells the dispatcher that it may dispatch a new job.
     * @param message The message containing the relevant harvester information.
     *
     */
    @Override
    public void visit(HarvesterReadyMessage message) {
        ArgumentNotValid.checkNotNull(message, "message");
        log.trace("Received ready message from " + message.getApplicationInstanceId());
        HarvestChannel channel = harvestChannelDao.getByName(message.getHarvestChannelName());
        jobDispatcher.submitNextNewJob(channel);
    }

    @Override
    public void visit(HarvesterRegistrationRequest msg) {
        ArgumentNotValid.checkNotNull(msg, "msg");
        
        String harvesterInstanceId = msg.getInstanceId();        
        String channelName = msg.getHarvestChannelName();

        boolean isSnapshot = true;
        boolean isValid = true;
        try {
            HarvestChannel chan = harvestChannelDao.getByName(channelName);
            isSnapshot = chan.isSnapshot();
        } catch (UnknownID e) {
            isValid = false;
        }

        if (isValid) {
        	harvestChannelRegistry.register(channelName, harvesterInstanceId);
        }

        // Send the reply
        jmsConnection.send(new HarvesterRegistrationResponse(channelName, isValid, isSnapshot));
        log.info("Sent a message to notify that harvest channel '" + channelName + "' is "
                + (isValid ? "valid." :  "invalid."));
    }

}
