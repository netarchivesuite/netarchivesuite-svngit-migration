/* File:        $Id  $
 * Revision:    $Revision $
 * Author:      $Author $
 * Date:        $Date $
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
package dk.netarkivet.common.utils.batch;

import java.io.File;
import java.io.OutputStream;
import java.util.Date;

/**
 * Job which returns the names of all files in the archive modified after a specific date.
 */
public class DatedFileListJob extends FileListJob {

    private Date since;

    /**
     * Constructor for this class.
     * @param since The date after which we require files to be listed.
     */
    public DatedFileListJob(Date since) {
        super();
        this.since = since;
    }

    /**
     * Writes the name of the arcfile to the OutputStream if its lastModified date is more recent than "since".
     * @param file an arcfile
     * @param os the OutputStream to which data is to be written
     * @return false If listing of this arcfile fails because of an error; true if the name is listed or if it is not
     * listed because the file is too old.
     */
    @Override
    public boolean processFile(File file, OutputStream os) {
        if (file.lastModified() > since.getTime()) {
            return super.processFile(file, os);
        } else {
            return true;
        }
    }
}
