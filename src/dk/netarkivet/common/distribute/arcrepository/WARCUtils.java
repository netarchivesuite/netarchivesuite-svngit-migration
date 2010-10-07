/* $Id$
 * $Date$
 * $Revision$
 * $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.common.distribute.arcrepository;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.io.ArchiveRecordHeader;
import org.archive.io.warc.WARCConstants;
import org.archive.io.warc.WARCRecord;

import dk.netarkivet.common.Constants;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;

/**
* Various utilities on WARC-records.
* We have borrowed code from wayback.
* @see org.archive.wayback.resourcestore.indexer.WARCRecordToSearchResultAdapter.java
*/
public class WARCUtils {
    
    /** Logging output place. */
    protected static final Log log = LogFactory.getLog(WARCUtils.class);

    /**
     * Read the contents of an ARC record into a byte array.
     * 
     * @param record
     *            An ARC record to read from. After reading, the ARC Record will
     *            no longer have its own data available for reading.
     * @return A byte array containing the contents of the ARC record. Note that
     *         the size of this may be different from the size given in the ARC
     *         record metadata.
     * @throws IOFailure
     *             If there is an error reading the data, or if the record is
     *             longer than Integer.MAX_VALUE (since we can't make bigger
     *             arrays).
     * @throws IOException If there is an error reading the data.            
     */
    public static byte[] readWARCRecord(WARCRecord record) throws IOException {
        ArgumentNotValid.checkNotNull(record, "WARCRecord record");
        if (record.getHeader().getLength() > Integer.MAX_VALUE) {
            throw new IOFailure("ARC Record too long to fit in array: "
                    + record.getHeader().getLength() + " > "
                    + Integer.MAX_VALUE);
        }

        // Skip to the ContentBlock of the WARCRecord
        ArchiveRecordHeader header = record.getHeader();
        int contentBegin = header.getContentBegin();
        long length = header.getLength();
        int dataLength = (int) (length - contentBegin); // we know that this doesn't go wrong due to the above check
        //record.skip(contentBegin);
        log.info("DataLength set to " + dataLength);
                
        // read from stream
        // The arcreader has a number of "features" that complicates the read
        // 1) the record at offset 0, returns too large a length
        // 2) readfully does not work
        // 3) ARCRecord.read(buf, offset, length) is broken.
        // TODO verify if these "features" are still around: See bugs #903,
        // #904,
        // #905
        //int dataLength = (int) record.getHeader().getLength();
        byte[] tmpbuffer = new byte[dataLength];
        byte[] buffer = new byte[Constants.IO_BUFFER_SIZE];
        int bytesRead;
        int totalBytes = 0;
        for (; (totalBytes < dataLength)
                && ((bytesRead = record.read(buffer)) != -1); totalBytes += bytesRead) {
            System.arraycopy(buffer, 0, tmpbuffer, totalBytes, bytesRead);
        }
        
        // Check if the number of bytes read (=i) matches the
        // size of the buffer.
        if (tmpbuffer.length != totalBytes) {
            // make sure we only return an array with bytes we actualy read
            byte[] truncateBuffer = new byte[totalBytes];
            System.arraycopy(tmpbuffer, 0, truncateBuffer, 0, totalBytes);
            log.debug("Storing " + totalBytes + " bytes. Expected to store: "
                    + tmpbuffer.length);
            return truncateBuffer;
        } else {
            return tmpbuffer;
        }

    }
    
    /**
     * Find out what type of WARC-record this is.
     * @param record a given WARCRecord
     * @return the type of WARCRecord as a String.
     */
    public static String getRecordType(WARCRecord record) {
        ArchiveRecordHeader header = record.getHeader();
        return (String) header.getHeaderValue(WARCConstants.HEADER_KEY_TYPE);
    }
    
}
