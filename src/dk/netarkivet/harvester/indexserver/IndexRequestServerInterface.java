/* $Id$
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *   USA
 */
package dk.netarkivet.harvester.indexserver;

import java.util.Set;
import dk.netarkivet.common.distribute.indexserver.RequestType;

/**
 * An interface for all IndexRequestServer implementations.
 */
public interface IndexRequestServerInterface {
    
    /**
     * Define a FileBasedCache class to handle the given type of requests.
     * @param type a given request type
     * @param cache the FileBasedCache class to handle this request type
     */
    void setHandler(RequestType type, FileBasedCache<Set<Long>> cache);
    /**
     * The operation to the start the IndexRequestServer.
     */
    void start();
    /**
     * The operation to the close the IndexRequestServer.
     * This closes all resources associated with the IndexRequestServer
     * and shuts down the server. 
     */
    void close();
}
