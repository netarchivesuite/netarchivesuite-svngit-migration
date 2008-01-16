/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

package dk.netarkivet.harvester.datamodel;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * A container for miscellaneous information about a TLD.
 *
 * Currently contains the TLD name and a count of subdomains.
 *
 */
public class TLDInfo implements Comparable {
    private final String tldName;
    private int count = 0;
    /** The special name for IP adresses, since they have no TLD */
    static final String IP_ADDRESS_NAME = "IP Address";

    /** Create TLD info holder.
     *
     * @param name The TLD domain name.
     */
    public TLDInfo(String name) {
        ArgumentNotValid.checkNotNullOrEmpty(name, "String name");
        tldName = name;
    }

    /** The name of this TLD (e.g. dk, com or museum).  IP addresses are
     * registered under a special "IP address" name.
     *
     * @return TLD name without .
     */
    public String getName() {
        return tldName;
    }

    /** Number of subdomains we have registered under this TLD.  All IP
     * addresses are lumped together as one TLD.
     *
     * @return Number of 2nd-level domains we have registered under this TLD.
     */
    public int getCount() {
        return count;
    }

    /** Add a 2nd-level domain to the information for this domain.
     *
     * This tests that the given domain does in fact belong to this TLD, but
     * not whether it has been added before.
     *
     * @param name
     */
    void addSubdomain(String name) {
        ArgumentNotValid.checkNotNullOrEmpty(name, "String name");
        if (tldName.equals(IP_ADDRESS_NAME)) {
            ArgumentNotValid.checkTrue(
                    dk.netarkivet.common.Constants.IP_KEY_REGEXP
                            .matcher(name).matches(),
                    "name must be an IP address");
        } else {
            ArgumentNotValid.checkTrue(name.endsWith("." + tldName),
                    "name must end with '." + tldName + "'");
        }
        count++;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TLDInfo tldInfo = (TLDInfo) o;

        if (!tldName.equals(tldInfo.tldName)) return false;

        return true;
    }

    public int hashCode() {
        return tldName.hashCode();
    }

    /**
     * Compares this object with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.<p>
     *
     * @see Comparable#compareTo(Object o)
     *
     * @param o the Object to be compared.
     * @return a negative integer, zero, or a positive integer as this object
     *         is less than, equal to, or greater than the specified object.
     * @throws ClassCastException if the specified object's type prevents it
     *                            from being compared to this Object.
     */
    public int compareTo(Object o) {
        TLDInfo i = (TLDInfo) o;
        return tldName.compareTo(i.tldName);
    }

    /** Get the TLD for a given domain.
     *
     * @param domain A domain, as specified by the global domain regexp.
     * @return The TLD of the domain, or a special placeholder for IP addresses.
     */
    static String getTLD(String domain) {
        ArgumentNotValid.checkNotNullOrEmpty(domain, "String domain");
        String tld;
        if (dk.netarkivet.common.Constants.IP_KEY_REGEXP.matcher(domain)
                .matches()) {
            tld = IP_ADDRESS_NAME;
        } else {
            // We know the format of domains, so we can assume a dot
            tld = domain.substring(domain.lastIndexOf('.') + 1);
        }
        return tld;
    }

    /** True if this TLDinfo accumulates IP address information.
     *
     * @return True if the domains counted in the TLDinfo are IP domains.
     */
    public boolean isIP() {
        return tldName.equals(IP_ADDRESS_NAME);
    }
}
