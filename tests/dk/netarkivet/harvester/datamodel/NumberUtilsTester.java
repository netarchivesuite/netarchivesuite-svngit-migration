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
package dk.netarkivet.harvester.datamodel;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

/**
 * Unit test for the utility class NumberUtils.
 * TODO Move class to the common package.
 * 
 */
public class NumberUtilsTester extends TestCase {
    public NumberUtilsTester(String s) {
        super(s);
    }

    public void setUp() {
    }

    public void tearDown() {
    }

    /** Test minimum where -1 means infinite */
    public void testMinInf() throws Exception {
        assertEquals("-1 is greater than all",
                     Long.MAX_VALUE, NumberUtils.minInf(-1L, Long.MAX_VALUE));
        assertEquals("-1 is greater than all",
                     Long.MAX_VALUE, NumberUtils.minInf(Long.MAX_VALUE, -1L));
        assertEquals("-1 is greater than all",
                     0L, NumberUtils.minInf(-1L, 0));
        assertEquals("-1 is greater than all",
                     0L, NumberUtils.minInf(0, -1L));
        assertEquals("-1 and -1 gives -1",
                     -1L, NumberUtils.minInf(-1L, -1L));
        assertEquals("On non-infinite, give smallest",
                     42L, NumberUtils.minInf(42L, 54L));
        assertEquals("On non-infinite, give smallest",
                     42L, NumberUtils.minInf(54L, 42L));
        assertEquals("On non-infinite, give smallest",
                     42L, NumberUtils.minInf(42L, 42L));
    }

    /** Test comparing where -1 means infinite */
    public void testCompareInf() throws Exception {
        assertEquals("-1 is greater than all",
                     1, NumberUtils.compareInf(-1L, Long.MAX_VALUE));
        assertEquals("-1 is greater than all",
                     -1, NumberUtils.compareInf(Long.MAX_VALUE, -1L));
        assertEquals("-1 is greater than all",
                     1, NumberUtils.compareInf(-1L, 0));
        assertEquals("-1 is greater than all",
                     -1, NumberUtils.compareInf(0, -1L));
        assertEquals("-1 and -1 are equal",
                     0, NumberUtils.compareInf(-1L, -1L));
        assertEquals("On non-infinite, compare",
                     -1, NumberUtils.compareInf(42L, 54L));
        assertEquals("On non-infinite, compare",
                     1, NumberUtils.compareInf(54L, 42L));
        assertEquals("On non-infinite, compare",
                     0, NumberUtils.compareInf(42L, 42L));
    }
    
    public void testToPrimitiveArray() {
        List<Double> emptyList = new ArrayList<Double>();
        double[] doubles = NumberUtils.toPrimitiveArray(emptyList);
        assertTrue(doubles.length == 0);
        List<Double> notEmptyList = new ArrayList<Double>();
        double the42double = 42L;
        double the22double = 22L;
        notEmptyList.add(Double.valueOf(the42double));
        notEmptyList.add(Double.valueOf(the22double));
        doubles = NumberUtils.toPrimitiveArray(notEmptyList);
        assertTrue(doubles.length == 2);
        boolean found42Value = false;
        boolean found22Value = false;
        double firstDouble = doubles[0]; 
        double secondDouble = doubles[1];
        if (firstDouble == the42double || secondDouble == the42double) {
            found42Value = true;
        }
        if (firstDouble == the22double || secondDouble == the22double) {
            found22Value = true;
        }
        
        assertTrue(found22Value);
        assertTrue(found42Value);
        
    }
    
    
    
}