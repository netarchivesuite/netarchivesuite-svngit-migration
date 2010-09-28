/**
 *
 */
package dk.netarkivet.harvester.harvesting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;

/**
 * This class generate a report that lists ARC files
 * along with the opening date, closing date (if file was properly closed),
 * and size in bytes.
 *
 * Here is a sample of such a file:
 *
 * [ARCFILE] [Opened] [Closed] [Size]
 *  5-1-20100720161253-00000-bnf_test.arc.gz "2010-07-20 16:12:53.698" "2010-07-20 16:14:31.792" 162928
 *
 * The file is named "arcfiles-report.txt" and is generated by parsing the
 * "heritrix.out" file located in the crawl directory. Useful lines match the
 * following examples:
 *
 * 2010-07-20 16:12:53.698 INFO thread-14 org.archive.io.WriterPoolMember.createFile() Opened /somepath/jobs/current/high/5_1279642368951/arcs/5-1-20100720161253-00000.arc.gz.open
 *
 * and
 *
 * 2010-07-20 16:14:31.792 INFO thread-29 org.archive.io.WriterPoolMember.close() Closed /somepath/jobs/current/high/5_1279642368951/arcs/5-1-20100720161253-00000-bnf_test.arc.gz, size 162928
 *
 * In order to have such messages output to heritrix.out,
 * the "heritrix.properties" file must contain the following, uncommented line:
 *
 * org.archive.io.arc.ARCWriter.level = INFO
 *
 * Note that these strings have changed between Heritrix version 1.14.3
 * and 1.14.4, so they might change again in the future.
 *
 */
class ArcFilesReportGenerator {

    /**
     * Stores the opening date, closing date and size of an ARC file.
     */
    class ArcFileStatus {

        String openedDate = "";
        String closedDate = "";
        long size = -1L;

        protected String getOpenedDate() {
            return openedDate;
        }

        protected void setOpenedDate(String openedDate) {
            this.openedDate = openedDate;
        }

        protected String getClosedDate() {
            return closedDate;
        }

        protected void setClosedDate(String closedDate) {
            this.closedDate = closedDate;
        }

        protected long getSize() {
            return size;
        }
        protected void setSize(long size) {
            this.size = size;
        }

        @Override
        public String toString() {
            return "\"" + openedDate + "\" \"" + closedDate + "\" "
            + Long.toString(size);
        }

    }

    private static Log log = LogFactory.getLog(ArcFilesReportGenerator.class);

    /**
     * Format used to parse and extract values from  lines of heritrix.out
     * pertaining to an ARC file opening.
     */
    public static final MessageFormat ARC_OPEN_FORMAT =
        new MessageFormat(
                "{0} INFO thread-{1} "
                + "org.archive.io.WriterPoolMember.createFile() Opened "
                + "{2}.open");

    /**
     * Format used to parse and extract values from lines of heritrix.out
     * pertaining to an ARC file closing.
     */
    public static final MessageFormat ARC_CLOSE_FORMAT =
        new MessageFormat(
                "{0} INFO thread-{1} "
                + "org.archive.io.WriterPoolMember.close() Closed {2}"
                + ", size {3}");

    /**
     * The name of the report file. It will be generated in the crawl directory.
     */
    public static final String REPORT_FILE_NAME = "arcfiles-report.txt";

    /**
     * The Heritrix crawl directory.
     */
    private File crawlDir;

    /**
     * Builds a ARC files report generator, given the Heritrix crawl directory.
     * @param crawlDir the Heritrix crawl directory.
     */
    ArcFilesReportGenerator(File crawlDir) {
        this.crawlDir = crawlDir;
    }

    /**
     * Parses heritrix.out and generates the ARC files report.
     * @return the generated report file.
     */
    protected File generateReport() {

        Map<String, ArcFileStatus> reportContents = parseHeritrixOut();

        File arcFilesReport = new File(crawlDir, REPORT_FILE_NAME);

        try {
            arcFilesReport.createNewFile();
            PrintWriter out = new PrintWriter(arcFilesReport) ;

            out.println("[ARCFILE] [Opened] [Closed] [Size]");

            for (String arcFileName : reportContents.keySet()) {
                ArcFileStatus afs = reportContents.get(arcFileName);
                out.println(arcFileName + " " + afs.toString());
            }

            out.close();
        } catch (IOException e) {
            throw new IOFailure("Failed to create " + REPORT_FILE_NAME, e);
        }

        return arcFilesReport;
    }

    /**
     * Parses the heritrix.out file and maps to every found ARC file an
     * {@link ArcFileStatus} instance.
     */
    protected Map<String, ArcFileStatus> parseHeritrixOut() {

        Map<String, ArcFileStatus> arcFiles =
            new LinkedHashMap<String, ArcFileStatus>();

        try {
            BufferedReader heritrixOut = new BufferedReader(
                    new FileReader(new File(crawlDir, "heritrix.out")));

            String line = null;
            while ((line = heritrixOut.readLine()) != null) {

                try {
                    Object[] params = ARC_OPEN_FORMAT.parse(line);

                    String openedDate = (String) params[0];
                    String arcFileName = new File((String) params[2]).getName();

                    ArcFileStatus afs = new ArcFileStatus();
                    afs.setOpenedDate(openedDate);

                    arcFiles.put(arcFileName, afs);
                } catch (ParseException e) {
                    // NOP, that's not the line we're looking for.
                }

                try {
                    Object[] params = ARC_CLOSE_FORMAT.parse(line);

                    String closedDate = (String) params[0];
                    String arcFileName = new File((String) params[2]).getName();
                    Long size = Long.parseLong((String) params[3]);

                    ArcFileStatus afs = arcFiles.get(arcFileName);
                    if (afs == null) {
                        throw new ArgumentNotValid("ARC file " + arcFileName +
                        " has no previous Opened record!");
                    }

                    afs.setClosedDate(closedDate);
                    afs.setSize(size);

                } catch (ParseException e) {
                    // NOP, that's not the line we're looking for.
                }

            }

            heritrixOut.close();
        } catch (IOException e) {
            log.error(e);
            return arcFiles;
        }

        return arcFiles;
    }

}
