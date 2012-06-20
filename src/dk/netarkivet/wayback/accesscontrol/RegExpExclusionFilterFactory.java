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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.netarkivet.wayback.accesscontrol;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.archive.wayback.accesscontrol.ExclusionFilterFactory;
import org.archive.wayback.resourceindex.filters.ExclusionFilter;
import org.archive.wayback.util.CloseableIterator;
import org.archive.wayback.util.flatfile.FlatFile;

/**
 * This class allows one to specify a file containing a list of regular
 * expressions specifying url's to be blocked from access via wayback.
 *
 * The class is intended to be instantiated as a Spring bean in a wayback
 * access point, for example by adding something like
 * <pre>
 * {@code
 *   <property name="exclusionFactory">
 *       <bean class="dk.netarkivet.wayback.accesscontrol.RegExpExclusionFilterFactory" init-method="init">
 *           <property name="file" value="/home/test/wayback_regexps.txt" />
 *       </bean>
 *   </property>
 * }
 * </pre>
 * to an access-point definition in wayback.xml.
 */
public class RegExpExclusionFilterFactory implements ExclusionFilterFactory {

    /**
     * Spring bean property specifying a flat file from which the regular
     * expressions are to be read.
     */
    private File file;

    /**
     * The collection of regular expressions to be checked
     */
    Collection<Pattern> patterns;

    /**
     * Initialiser to be called from Spring framework.
     * @throws IOException if the file specifying the exclusions cannot be read.
     * @throws PatternSyntaxException if one or more of the patterns in the
     * configuration file is an invalid java regular expression.
     */
    public void init() throws IOException, PatternSyntaxException {
        loadFile();
    }

    /**
     * Reads the file containing the regular expressions to be used as a filter,
     * ignoring any blank lines or leading and trailing whitespace.
     * @throws IOException if the file cannot be read.
     * @throws PatternSyntaxException if one or more of the patterns in the
     * configuration file is an invalid java regular expression.
     */
    private void loadFile() throws IOException, PatternSyntaxException {
        Collection<Pattern> regexps = new ArrayList<Pattern>();
        FlatFile ff = new FlatFile(file.getAbsolutePath());
        CloseableIterator<String> itr = ff.getSequentialIterator();
        while(itr.hasNext()) {
            String line = (String) itr.next();
            line = line.trim();
            if(line.length() == 0) {
                continue;
            }
            regexps.add(Pattern.compile(line));
        }
        this.patterns = regexps;
    }


    /**
     * Get the file from which regexps are read.
     * @return the file.
     */
    public File getFile() {
        return file;
    }

    /**
     * Set the file from which regexps are read.
     * @param file thefile.
     */
    public void setFile(File file) {
        this.file = file;
    }

    @Override
    public ExclusionFilter get() {
        return new RegExpExclusionFilter(patterns);
    }

    @Override
    public void shutdown() {
        //Nothing to do
    }
}
