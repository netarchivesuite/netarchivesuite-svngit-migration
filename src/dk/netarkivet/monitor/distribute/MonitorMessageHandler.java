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
package dk.netarkivet.monitor.distribute;

import javax.jms.Message;
import javax.jms.MessageListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.monitor.registry.distribute.RegisterHostMessage;

/**
 * This default message handler shields of all unimplemented methods from the
 * MonitorMessageVisitor interface.
 *
 * Classes should not implement MonitorMessageVisitor but extend this class.
 *
 * @see MonitorMessageVisitor
 *
 */
public abstract class MonitorMessageHandler
        implements MonitorMessageVisitor, MessageListener {

    private final Log log = LogFactory.getLog(getClass().getName());

    /**
     * Creates a MonitorMessageHandler object.
     */
    public MonitorMessageHandler() {
    }

    /**
     * Unpacks and calls accept() on the message object.
     *
     * This method catches <b>all</b> exceptions and logs them.
     *
     * @param msg a ObjectMessage
     *
     */
    public void onMessage(Message msg) {
        ArgumentNotValid.checkNotNull(msg, "Message msg");
        log.trace("Message received:\n" + msg.toString());
        try {
            ((MonitorMessage) JMSConnection.unpack(msg)).accept(this);
        } catch (ClassCastException e) {
            log.warn("Invalid message type", e);
        } catch (Throwable e) {
            log.warn("Error processing message '" + msg + "'", e);
        }
    }

    /** Handles when a handler receives a message it is not prepare to handle.
     *
     * @param msg The received message.
     * @throws PermissionDenied Always
     */
    private void deny(MonitorMessage msg) {
        throw new PermissionDenied("'" + this + "' provides no handling for "
                + msg + " of type " + msg.getClass().getName()
                + " and should not be invoked!");
    }

    /**
     * This method should be overridden and implemented by a sub class if
     * message handling is wanted.
     * @param msg a RegisterHostMessage
     * @throws PermissionDenied when invoked
     */
    public void visit(RegisterHostMessage msg) throws PermissionDenied {
        ArgumentNotValid.checkNotNull(msg, "RegsiterHostMessage msg");
        deny(msg);
    }
}