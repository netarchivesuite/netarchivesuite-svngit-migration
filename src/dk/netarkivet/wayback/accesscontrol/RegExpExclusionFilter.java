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
 *
 */
package dk.netarkivet.wayback.accesscontrol;

import java.util.Collection;
import java.util.regex.Pattern;

import org.archive.wayback.core.CaptureSearchResult;
import org.archive.wayback.resourceindex.filters.ExclusionFilter;
import org.archive.wayback.util.ObjectFilter;

public class RegExpExclusionFilter extends ExclusionFilter {

    /**
     * The regexps to be used as the exclusion filter.
     */
    Collection<Pattern> regexps;

    /**
     * Creates an exclusion filter which will filter out any search result for
     * which the original url matches any of the specified regular expression.
     * @param regexps The regular expressions to match.
     */
    public RegExpExclusionFilter(Collection<Pattern> regexps) {
        this.regexps = regexps;
    }

    @Override
    public int filterObject(CaptureSearchResult captureSearchResult) {
        // Note that the behaviour of the two calls to methods of the class
        // ExclusionCaptureFilterGroup is not well documented. Omitting them
        // results in the excluded objects being marked as not in the archive.
        // With these calls, they are correctly identified as blocked.
        filterGroup.setSawAdministrative();
        for (Pattern regexp: regexps) {
            if (regexp.matcher(captureSearchResult.getOriginalUrl()).matches()) {
                return ObjectFilter.FILTER_EXCLUDE;
            }
        }
        filterGroup.setPassedAdministrative();
        return ObjectFilter.FILTER_INCLUDE;
    }
}
