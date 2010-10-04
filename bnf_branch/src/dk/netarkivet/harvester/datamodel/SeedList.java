/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

package dk.netarkivet.harvester.datamodel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.Named;


/**
 * Representation of the list of harvesting seeds.
 * Basically just a list of URL's
 *
 */
public class SeedList implements Serializable, Named {
    /** The name of the seedlist. Used for sorting. */
    private String name;
    /** The List of Seeds; Each String in the List holds one seed. */
    private List<String> seeds;
    /** Any comments associated with this seedlist. */
    private String comments;

    /** ID autogenerated by DB, ignored otherwise. */
    private Long id;

    /**
     * Create new seedlist. Helper constructor that takes the seeds as a
     * newline separated string.
     * @param name the name of the new seedlist
     * @param seedsAsString the seeds
     * @throws ArgumentNotValid if name is null or empty
     * or seeds are null. Empty seeds are allowed.
     */
    public SeedList(String name, String seedsAsString) {
        ArgumentNotValid.checkNotNullOrEmpty(name, "name");
        ArgumentNotValid.checkNotNull(seedsAsString, "seeds");

        BufferedReader urlreader = new BufferedReader(
                new StringReader(seedsAsString));
        seeds = new LinkedList<String>();
        String url;
        try {
            while ((url = urlreader.readLine()) != null) {
                if (isAcceptableURL(url)) {
                    seeds.add(url);
                } else {
                    throw new ArgumentNotValid("The URL '" + url
                                               + "' is not valid");
                }
            }
        } catch (IOException e) {
            throw new IOFailure("Should never happen: "
                                + "IO Failure while reading a string", e);
        }
        this.name = name;
        this.comments = "";
    }

    /** Check urls for validity. Currently accepts valid URL specs.
     *
     * @param url The url to check
     * @return true, if it is accepted
     */
    private boolean isAcceptableURL(String url) {
        if (url.isEmpty()) {
            return false;
        }
        try {
            URL validUrl = new URL(url);
        } catch (MalformedURLException e) {
            return false;
        }
        return true;
    }

    /**
     * Create new seedlist.
     * @param name the name of the new seedlist
     * @param seeds the seeds
     * @throws ArgumentNotValid if the arguments are null or empty strings
     */
    public SeedList(String name, List<String> seeds) {
        ArgumentNotValid.checkNotNullOrEmpty(name, "name");
        ArgumentNotValid.checkNotNullOrEmpty(seeds, "seeds");

        this.name = name;
        this.seeds = seeds;
        this.comments = "";
    }

    /** Gets all seeds in a list.
     *
     * @return The seeds
     */
    public List<String> getSeeds() {
        return seeds;
    }

    /**
     * Gets the seeds. Seeds are separated by newline,
     * @return the seedlist as a String
     */
    public String getSeedsAsString() {
        StringWriter urls = new StringWriter();
        PrintWriter urlwriter = new PrintWriter(urls);
        for (Iterator<String> i = seeds.iterator(); i.hasNext();) {
            String url = i.next();
            urlwriter.println(url);
        }
        urlwriter.flush();
        String tmp = urls.toString();
        try {
            urls.close();
        } catch (IOException e) { }
        return tmp;
    }

    /**
     * Get the name of this seedlist.
     *
     * @return The name.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the comments of this seedlist.
     *
     * @return The comments.
     */
    public String getComments() {
        return comments;
    }

    /** Set the comments for this list.
     *
     * @param s User-entered free-form comments.
     */
    public void setComments(String s) {
        comments = s;
    }


    /** Get the ID of this seedlist.  Only for use by DBDAO
     * @return the ID of this seedlist
     */
    long getID() {
        return id;
    }

    /** Set the ID of this seedlist.  Only for use by DBDAO
     * @param id the new ID of this seedlist
     */
    void setID(long id) {
        this.id = id;
    }

    /** Check if this seedlist has an ID set yet (doesn't happen until
     * the DBDAO persists it).
     * @return true if this seedlist has an ID set
     */
    boolean hasID() {
        return id != null;
    }

    /**
     * Returns a human-readable representation of the seeds.
     * @return A readable string
     */
    public String toString() {
        return getName() + ": " + seeds.toString();
    }

    /**
     * Auto generated by IntelliJ IDEA.
     * @param o The object to compare with
     * @return Whether they are equal
     */
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SeedList)) {
            return false;
        }

        final SeedList seedList = (SeedList) o;

        if (!comments.equals(seedList.comments)) {
            return false;
        }
        if (!name.equals(seedList.name)) {
            return false;
        }
        if (!seeds.equals(seedList.seeds)) {
            return false;
        }

        return true;
    }

    /**
     * Auto generated by IntelliJ IDEA.
     * @return hashcode
     */
    public int hashCode() {
        int result;
        result = name.hashCode();
        result = 29 * result + seeds.hashCode();
        result = 29 * result + comments.hashCode();
        return result;
    }
}
