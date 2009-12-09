/* $Id$
 * $Revision$
 * $Author$
 * $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import java.util.Date;

/**
 * This class manages owner information about a domain. Immutable.
 */
public class DomainOwnerInfo implements Comparable {

    /** Information about the owner of a domain. */
    private String ownerInfo;

    /** The date the information was registered. */
    private Date date;

    /** ID autogenerated by DB, ignored otherwise. */
    private Long id;

    /**
     * Create new instance.
     *
     * @param d the date the owner information is registered
     * @param info the owner information
     */
    public DomainOwnerInfo(Date d, String info) {
        ArgumentNotValid.checkNotNull(d, "d");
        ArgumentNotValid.checkNotNullOrEmpty(info, "info");
        ownerInfo = info;
        date = d;
    }

    /**
     * Gets the owner information.
     *
     * @return the owner information
     */
    public String getInfo() {
        return ownerInfo;
    }

    /**
     * Gets the date.
     *
     * @return the date
     */
    public Date getDate() {
        return date;
    }

    /**
     * Get the ID of this ownerinfo. Only for use by DBDAO
     *
     * @return the ID of this ownerinfo object
     */
    long getID() {
        return id;
    }

    /**
     * Set the ID of this ownerinfo. Only for use by DBDAO.
     *
     * @param id
     *            use this id for this ownerinfo
     */
    void setID(long id) {
        this.id = id;
    }

    /**
     * Check if this ownerinfo has an ID set yet (doesn't happen until the DBDAO
     * persists it).
     *
     * @return true, if this ownerinfo-object has an ID
     */
    boolean hasID() {
        return id != null;
    }

    /**
     * Compares two DomainOwnerInfo objects using dates.
     *
     * @param other is a non-null DomainOwnerInfo.
     * @throws ClassCastException
     *             if <code>other</code> is not an DomainOwnerInfo object.
     * @see Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object other) {
        ArgumentNotValid.checkNotNull(other, "other");

        if (this == other) {
            return 0;
        }
        return date.compareTo(((DomainOwnerInfo) other).date);
    }
}
