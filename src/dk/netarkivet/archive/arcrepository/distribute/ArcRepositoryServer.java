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
package dk.netarkivet.archive.arcrepository.distribute;

import java.io.File;
import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.archive.arcrepository.ArcRepository;
import dk.netarkivet.archive.arcrepository.bitpreservation.AdminDataMessage;
import dk.netarkivet.archive.bitarchive.distribute.BatchMessage;
import dk.netarkivet.archive.bitarchive.distribute.BatchReplyMessage;
import dk.netarkivet.archive.bitarchive.distribute.GetFileMessage;
import dk.netarkivet.archive.bitarchive.distribute.GetMessage;
import dk.netarkivet.archive.bitarchive.distribute.RemoveAndGetFileMessage;
import dk.netarkivet.archive.bitarchive.distribute.UploadMessage;
import dk.netarkivet.archive.checksum.distribute.CorrectMessage;
import dk.netarkivet.archive.checksum.distribute.GetAllChecksumsMessage;
import dk.netarkivet.archive.checksum.distribute.GetAllFilenamesMessage;
import dk.netarkivet.archive.checksum.distribute.GetChecksumMessage;
import dk.netarkivet.archive.distribute.ArchiveMessageHandler;
import dk.netarkivet.archive.distribute.ReplicaClient;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.Settings;

/**
 * Listens on the queue "TheArcrepos" and submits the messages to
 * a corresponding visit method on BitarchiveClient.
 */
public class ArcRepositoryServer extends ArchiveMessageHandler {
    /** The log.*/
    private final Log log = LogFactory.getLog(getClass());
    /** The ArcRepository connected to this server.*/
    private final ArcRepository ar;

    /**
     * Creates and adds a ArcRepositoryMessageHandler as listener on
     * the "TheArcrepos"-queue.
     * @param ar the ArcRepository
     */
    public ArcRepositoryServer(ArcRepository ar) {
        ArgumentNotValid.checkNotNull(ar, "ArcRepository ar");
        this.ar = ar;
        ChannelID channel = Channels.getTheRepos();
        log.info("Listening for arc repository messages on channel '"
                 + channel + "'");
        JMSConnectionFactory.getInstance().setListener(channel, this);
    }

    /**
     * Forwards the call to the ArcRepository.store() method with
     * the StoreMessage as parameter.
     * In case of exception when calling store, a reply message is sent
     * containing the message set as NotOK.
     *
     * @param msg the message to be processed by the store command.
     */
    public void visit(StoreMessage msg) {
        ArgumentNotValid.checkNotNull(msg, "msg");
        try {
            ar.store(msg.getRemoteFile(), msg);
        } catch (Throwable t) {
            log.warn("Failed to handle store request", t);
            msg.setNotOk(t);
            JMSConnectionFactory.getInstance().reply(msg);
        }
    }

    /**
     * Request a file to be deleted from bitarchives. This request will be
     * handled by the bitarchives, and the bitarchive containing the file
     * will reply with the removed file if succesful, or with a notOk message
     * if unsuccesful.
     *
     * Will send a not-ok reply on exceptions handling this request.
     *
     * @param msg the message to be processed
     */
    public void visit(RemoveAndGetFileMessage msg) {
        ArgumentNotValid.checkNotNull(msg, "msg");

        try {
            ar.removeAndGetFile(msg);
        } catch (Throwable t) {
            log.warn("Failed to handle request to remove file", t);
            msg.setNotOk(t);
            JMSConnectionFactory.getInstance().reply(msg);
        }
    }

    /**
     * Update the admin data in the arcrepository. Reply aftwerwards. Will reply
     * with notOk on exceptions.
     * 
     * @param msg the message to be processed
     */
    public void visit(AdminDataMessage msg) {
        ArgumentNotValid.checkNotNull(msg, "msg");

        try {
            ar.updateAdminData(msg);
            JMSConnectionFactory.getInstance().reply(msg);
        } catch (Throwable t) {
            log.warn("Failed to handle request to change admin data", t);
            msg.setNotOk(t);
            JMSConnectionFactory.getInstance().reply(msg);
        }
    }

    /**
     * Forwards the handling of upload replies to the arc repository.
     * Will log errors, but otherwise ignore.
     *
     * @param msg a UploadMessage
     * @throws ArgumentNotValid If the message is null.
     */
    public void visit(UploadMessage msg) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(msg, "UploadMessage msg");
        try {
            ar.onUpload(msg);
        } catch (Throwable t) {
            log.warn("Failed to handle upload reply", t);
        }
    }

    /**
     * Forwards the handling of batch replies to the arc repository.
     * Will log errors, but otherwise ignore.
     *
     * @param msg a BatchReplyMessage
     * @throws ArgumentNotValid If the message is null.
     */
    public void visit(BatchReplyMessage msg) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(msg, "BatchReplyMessage msg");
        
        try {
            ar.onBatchReply(msg);
        } catch (Throwable t) {
            log.warn("Failed to handle batch reply", t);
        }
    }

    /** Resends a batch message to the requested bitarchive.
     *
     * Note that this circumvents the ArcRepository entirely and that the
     * reply goes directly back to whoever set the message.
     *
     * @param msg the batch message to be resend.
     * @throws ArgumentNotValid if parameters are null
     */
    public void visit(BatchMessage msg) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(ar, "ar");
        ArgumentNotValid.checkNotNull(msg, "BatchMessage msg");

        try {
            ReplicaClient rc = ar.getReplicaClientFromReplicaId(
                    msg.getReplicaId());
            rc.sendBatchJob(msg);
        } catch (Throwable t) {
            log.warn("Failed to handle batch request", t);
            BatchReplyMessage replyMessage = new BatchReplyMessage(
                    msg.getReplyTo(), Channels.getTheRepos(), msg.getID(),
                    0, Collections.<File>emptyList(), null);
            replyMessage.setNotOk(t);
            JMSConnectionFactory.getInstance().send(replyMessage);
        }
    }

    /** Forwards a get message to the local bitarchive.
     *
     * Note that this circumvents the ArcRepository entirely and that the
     * reply goes directly back to whoever sent the message.
     *
     * @param msg the message to be processed by the get command.
     * @throws ArgumentNotValid If the message is null.
     */
    public void visit(GetMessage msg) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(msg, "GetMessage msg");

        try {
            ReplicaClient rc = ar.getReplicaClientFromReplicaId(
                Settings.get(CommonSettings.USE_REPLICA_ID));
            rc.sendGetMessage(msg);
        } catch (Throwable t) {
            log.warn("Failed to handle get request", t);
            msg.setNotOk(t);
            JMSConnectionFactory.getInstance().reply(msg);
        }
    }

    /** Forwards a getfile message to requested bitarchive replica.
     *
     * Note that this circumvents the ArcRepository entirely and that the
     * reply goes directly back to whoever set the message.
     *
     * @param msg the message to be processed by the get command.
     * @throws ArgumentNotValid If one of the arguments are null.
     */
    public void visit(GetFileMessage msg) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(msg, "GetFileMessage msg");

        try {
            ReplicaClient rc =
                ar.getReplicaClientFromReplicaId(msg.getReplicaId());
            rc.sendGetFileMessage(msg);
        } catch (Throwable t) {
            log.warn("Failed to handle get file request", t);
            msg.setNotOk(t);
            JMSConnectionFactory.getInstance().reply(msg);
        }
    }
    
    /**
     * For retrieving all the filenames from a replica.
     * 
     * @param msg The message to be processed.
     * @throws ArgumentNotValid If the argument is null.
     */
    public void visit(GetAllFilenamesMessage msg) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(msg, "GetAllFilenames msg");
        
        try {
            // retrieve the checksum client
            ReplicaClient rc = ar.getReplicaClientFromReplicaId(
                    msg.getReplicaId());
            rc.sendGetAllFilenamesMessage(msg);
        } catch (Throwable t) {
            log.warn("Failed to handle GetAllFilenamesMessage: " + msg, t);
            msg.setNotOk(t);
            JMSConnectionFactory.getInstance().reply(msg);
        }
    }
    
    /**
     * Method for retrieving all the checksums from a replica.
     * 
     * @param msg The GetAllChecksumsMessage.
     * @throws ArgumentNotValid If the GetAllChecksumsMessage is null.
     */
    public void visit(GetAllChecksumsMessage msg) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(msg, "GetAllChecksumsMessage msg");
        
        try {
            // retrieve the checksum client
            ReplicaClient rc = ar.getReplicaClientFromReplicaId(
                    msg.getReplicaId());
            rc.sendGetAllChecksumsMessage(msg);
        } catch (Throwable t) {
            log.warn("Failed to handle GetAllChecksumsMessage: " + msg, t);
            msg.setNotOk(t);
            JMSConnectionFactory.getInstance().reply(msg);
        }
    }
    
    /**
     * Method for handling the results of a GetChecksumMessage.
     * This should be handled similar to a ReplyBatchMessage, when a batchjob
     * has run on a single file.
     * 
     * @param msg The GetChecksumMessage message.
     */
    public void visit(GetChecksumMessage msg) {
        ArgumentNotValid.checkNotNull(msg, "GetChecksum msg");
        
        log.info("Received GetChecksumMessage '" + msg + "'.");
        
        // If it is a reply, then handle by arc-repository. 
        // Otherwise send further.
        if(msg.getIsReply()) {
            try {
                ar.onChecksumReply(msg);
            } catch(Throwable t) {
                log.warn("Failed to handle GetChecksumMessage", t);
            }
        } else {
            try {
                ReplicaClient rc = ar.getReplicaClientFromReplicaId(
                        msg.getReplicaId());
                rc.sendGetChecksumMessage(msg);
            } catch (Throwable e) {
                log.warn("Failed to handle GetChecksumMessage.", e);
                msg.setNotOk(e);
                JMSConnectionFactory.getInstance().reply(msg);
            }
        }
    }
    
    /**
     * Method for handling CorrectMessages. This message is just sent along to
     * the corresponding replica archive, where the 'bad' entry will be 
     * corrected (made backup of and then replaced).
     * 
     * @param msg The message for correcting a bad entry in an archive.
     * @throws ArgumentNotValid If the CorrectMessage is null.
     */
    public void visit(CorrectMessage msg) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(msg, "CorrectMessage msg");
        log.debug("Receiving CorrectMessage: " + msg.toString());
        
        try {
            ReplicaClient rc = ar.getReplicaClientFromReplicaId(
                    msg.getReplicaId());
            rc.sendCorrectMessage(msg);
        } catch (Throwable e) {
            log.warn("Could not handle Correct message properly.", e);
            msg.setNotOk(e);
            JMSConnectionFactory.getInstance().reply(msg);
        }
    }

    /**
     * Removes the ArcRepositoryMessageHandler as listener.
     */
    public void close() {
        JMSConnectionFactory.getInstance().removeListener(
                Channels.getTheRepos(), this);
    }
}
