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
package dk.netarkivet.harvester.harvesting;

import junit.framework.TestCase;
import org.archive.util.SurtPrefixSet;
import dk.netarkivet.harvester.harvesting.OnNSDomainsDecideRule;

/**
 * JUNIT test for the class OnNSDomainsDecideRule.
 */
public class OnNSDomainsDecideRuleTester extends TestCase {
    public OnNSDomainsDecideRuleTester(String s) {
        super(s);
    }

    public void setUp() {
    }

    public void tearDown() {
    }

    public void testSURTprefixConversionDomains() throws Exception {

        /*
         First testing that the original way of making URls to SURTs behaves right on domains
         */
        // FIXME SurtPrefixSet.prefixFromPlain method replaced by prefixFromPlainForceHttp
        // Is this method OK to use instead
        assertEquals("http://(dk,dr,www,",SurtPrefixSet.prefixFromPlainForceHttp(("www.dr.dk")));

        assertEquals("http://(dk,dr,",SurtPrefixSet.prefixFromPlainForceHttp("http://dr.dk"));

        /*
         Then testing using OnNSDomainsDecideRule - that defines the domain
         using DomainNameQueueAssignmentPolicy
         */

        OnNSDomainsDecideRule oddr = new OnNSDomainsDecideRule("");

        assertEquals("http://(dk,dr,",oddr.prefixFrom("http://www.dr.dk"));

        assertEquals("http://(dk,dr,",oddr.prefixFrom("http://www.dr.dk/sporten/index.php"));

        assertEquals("http://(dk,dr,",oddr.prefixFrom("http://www.dr.dk/sporten/"));

        assertEquals("http://(dk,tv2,",oddr.prefixFrom("http://sporten.tv2.dk/fodbold/"));

        assertEquals("http://(uk,co,sports,",oddr.prefixFrom("http://www.sports.co.uk/index"));

    }

    public void testSURTprefixConversionHosts() throws Exception {

        /*
         Testing using the convertPrefixToHost used by OnHostsDecideRule
         // FIXME SurtPrefixSet.prefixFromPlain method replaced by prefixFromPlainForceHttp
        // Is this method OK to use instead
         */
        assertEquals("http://(dk,tv2,sporten,)",
                SurtPrefixSet.convertPrefixToHost(
                        SurtPrefixSet.prefixFromPlainForceHttp("http://sporten.tv2.dk/fodbold/")));

        assertEquals("http://(com,blogspot,jmvietnam07,)",
                SurtPrefixSet.convertPrefixToHost(
                        SurtPrefixSet.prefixFromPlainForceHttp("jmvietnam07.blogspot.com/")));

    }

    public void testSURTprefixConversionPaths() throws Exception {

        /*
         Testing using the 'original' way of transforming URLs to SURTs
         used by SurtPrefixesDecideRule
         
         // FIXME SurtPrefixSet.prefixFromPlain method replaced by prefixFromPlainForceHttp
        // Is this method OK to use instead
         */

        assertEquals("http://(com,geocities,www,)/athens/2344/",
                SurtPrefixSet.prefixFromPlainForceHttp("http://www.geocities.com/Athens/2344/"));

        assertEquals("http://(com,geocities,www,)/athens/2344/",
                SurtPrefixSet.prefixFromPlainForceHttp("http://www.geocities.com/Athens/2344/index.php"));

    }

    public void testSURTprefixConversionNonValidDomain() throws Exception {

     // FIXME SurtPrefixSet.prefixFromPlain method replaced by prefixFromPlainForceHttp
        // Is this method OK to use instead
        assertEquals(OnNSDomainsDecideRule.NON_VALID_DOMAIN,
                SurtPrefixSet.convertPrefixToHost(
                        SurtPrefixSet.prefixFromPlainForceHttp("http:/not?valid;bla%¤/(")));

    }

}
