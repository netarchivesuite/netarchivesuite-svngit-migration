/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package dk.netarkivet.wayback.batch;

import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.io.arc.ARCRecord;
import org.archive.wayback.UrlCanonicalizer;
import org.archive.wayback.core.CaptureSearchResult;
import org.archive.wayback.resourceindex.cdx.SearchResultToCDXLineAdapter;
import dk.netarkivet.common.Constants;
import dk.netarkivet.common.utils.arc.ARCBatchJob;
import dk.netarkivet.common.utils.batch.ARCBatchFilter;
import dk.netarkivet.wayback.batch.copycode.NetarchiveSuiteARCRecordToSearchResultAdapter;

/**
 * Returns a cdx file using the appropriate format for wayback, including
 * canonicalisation of urls. The returned files are unsorted.
 *
 */
public class WaybackCDXExtractionARCBatchJob extends ARCBatchJob {
   /**
     * Logger for this class.
     */
    private final Log log = LogFactory.getLog(getClass().getName());

    /**
     * Utility for converting an ArcRecord to a CaptureSearchResult
     * (wayback's representation of a CDX record).
     */
    private NetarchiveSuiteARCRecordToSearchResultAdapter aToSAdapter;

    
    
    /**
     * Utility for converting a wayback CaptureSearchResult to a String
     * representing a line in a CDX file.
     */
    private SearchResultToCDXLineAdapter srToCDXAdapter;

     /**
     * Constructor which set timeout to one day.
     */
    public WaybackCDXExtractionARCBatchJob() {
        batchJobTimeout = Constants.ONE_DAY_IN_MILLIES;
    }

    /**
     * Constructor.
     * @param timeout specific timeout period
     */
    public WaybackCDXExtractionARCBatchJob(long timeout) {
        batchJobTimeout = timeout;
    }


    /**
     *  Initializes the private fields of this class. Some of these are
     *  relatively heavy objects, so it is important that they are only
     *  initialised once.
     * @param os unused argument
     */
    @Override
    public void initialize(OutputStream os) {
        log.info("Starting a " + this.getClass().getName());
        aToSAdapter = new NetarchiveSuiteARCRecordToSearchResultAdapter();
        UrlCanonicalizer uc = UrlCanonicalizerFactory
                .getDefaultUrlCanonicalizer();
        aToSAdapter.setCanonicalizer(uc);
        srToCDXAdapter = new SearchResultToCDXLineAdapter();
    }

    /**
     * For each ARCRecord writes one CDX line (including newline) to the output.
     * If an arcrecord cannot be converted to a CDX record for any reason then
     * any resulting exception is caught and logged.
     * @param record the ARCRecord to be indexed.
     * @param os the OutputStream to which output is written.
     */
    @Override
    public void processRecord(ARCRecord record, OutputStream os) {
        CaptureSearchResult csr = null;
        log.debug("Entered " + this.getClass().getName() + " for '" + record.getHeaderString() + "'");
        try {
            log.debug("Adapting Record '" + record.getHeader() + "'");
            csr = aToSAdapter.adapt(record);
            log.debug("Adapted Record '" + record.getHeader() + "' to '" + csr + "'");
        } catch (Exception e) {
            log.info(e);
            return;
        }
        try {
            if (csr != null) {
                log.debug("Adapting Search Result'" + csr + "'");
                String cdx = srToCDXAdapter.adapt(csr);
                os.write(cdx.getBytes());
                os.write("\n".getBytes());
                log.debug("Adapted Search Result '" + csr + "' + to '" + cdx + "'");
            } else {
                String message = "Could not parse '" + record.getHeaderString() + "'";
                log.info(message);
            }
        } catch (Exception e) {
            log.info(e);
        }
    }

    /**
     * Does nothing except log the end of the job.
     * @param os unused argument.
     */
    public void finish(OutputStream os) {
        log.info("Finishing the " + this.getClass().getName());
        //No cleanup required
    }
    
    @Override
    public ARCBatchFilter getFilter() {
        return ARCBatchFilter.EXCLUDE_FILE_HEADERS;
    }
    
}
