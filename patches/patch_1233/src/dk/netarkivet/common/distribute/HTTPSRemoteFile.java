/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.netarkivet.common.distribute;

import java.io.File;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;

/**
 * A remote file implemented with point-to-point HTTPS communication.
 * Optimised to communicate locally, if file is on the same host.
 * Optimised to transfer 0 byte files inline.
 * Will use one shared certificate for secure communication.
 */
public class HTTPSRemoteFile extends HTTPRemoteFile {
    /**
     * Initialises a remote file implemented by point-to-point HTTPS
     * communication.
     *
     * @param file              The file to make a remote file for
     * @param useChecksums      Whether communications are checksummed. If true,
     *                          getChecksum will also return the checksum.
     * @param fileDeletable     if true, the file given to this method is
     *                          deletable, once it is transferred.
     * @param multipleDownloads if true, the file may be transferred more than
     *                          once. Otherwise, all file handles are attempted
     *                          to be made invalid after the first transfer,
     *                          although no guarantees are made.
     *
     * @throws ArgumentNotValid if file is null, or not a readable file.
     * @throws IOFailure        if checksums are requested, but i/o errors occur
     *                          while checksumming.
     */
    protected HTTPSRemoteFile(File file, boolean useChecksums,
                              boolean fileDeletable,
                              boolean multipleDownloads) {
        super(file, useChecksums, fileDeletable, multipleDownloads);
    }

    /**
     * Initialises a remote file implemented by point-to-point HTTPS
     * communication.
     * @param f The file to make a remote file for
     * @param useChecksums Whether communications are checksummed. If true,
     * getChecksum will also return the checksum.
     * @param fileDeletable if true, the file given to this method is deletable,
     * once it is transferred.
     * @param multipleDownloads if true, the file may be transferred more than
     * once. Otherwise, all file handles are attempted to be made invalid after
     * the first transfer, although no guarantees are made.
     * @throws ArgumentNotValid if file is null, or not a readable file.
     * @throws IOFailure if checksums are requested, but i/o errors occur while
     * checksumming.
     */
    public static RemoteFile getInstance(File f, Boolean useChecksums,
                                         Boolean fileDeletable,
                                         Boolean multipleDownloads) {
        return new HTTPSRemoteFile(f, useChecksums, fileDeletable,
                                   multipleDownloads);
    }


    /**
     * Get the HTTPS serving registry for remote files. Overrides the HTTP
     * registry use by HTTPRemoteFile.
     *
     * @return registry for remote files.
     */
    protected HTTPRemoteFileRegistry getRegistry() {
        return HTTPSRemoteFileRegistry.getInstance();
    }


}