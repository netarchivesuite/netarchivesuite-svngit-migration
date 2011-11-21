/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2011 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
 * USA
 */

package dk.netarkivet.viewerproxy.webinterface;

/**
 * Viewerproxy webinterface constants.
 */
public class Constants {
    
    /**
     * The contructor for this class. Making the constructor private prevents
     * the class from being instantiated.
     */
    private Constants() {
    }
    
    /** Names of various parameters used in the webinterface. */
    
    /** The jobid parameter. */
    public static final String JOBID_PARAM = "jobid"; 
    
    /** The domain attribute. */
    public static final String DOMAIN_PARAM = "domain"; 
    
    /** The regexp attribute. */
    public static final String REGEXP_PARAM = "regexp";
   }
