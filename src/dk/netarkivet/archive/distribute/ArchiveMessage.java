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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 
 *  USA
 */

package dk.netarkivet.archive.distribute;

import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.NetarkivetMessage;
import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Common base class for messages exchanged between an archive server and an
 * archive client (or within an archive).
 *
 * @see NetarkivetMessage
 */
public abstract class ArchiveMessage extends NetarkivetMessage {
    /**
     * Creates a new ArchiveMessage.
     *
     * @param to        the initial receiver of the message
     * @param replyTo   the initial sender of the message
     * @throws ArgumentNotValid if to==replyTo or there is a null parameter.
     */
    protected ArchiveMessage(ChannelID to, ChannelID replyTo) 
            throws ArgumentNotValid {
        super(to, replyTo);
    }

    /**
     * Should be implemented as a part of the visitor pattern. e.g.: public void
     * accept(ArchiveMessageVisitor v) { v.visit(this); }
     *
     * @see ArchiveMessageVisitor
     *
     * @param v A message visitor
     */
    public abstract void accept(ArchiveMessageVisitor v);
}
