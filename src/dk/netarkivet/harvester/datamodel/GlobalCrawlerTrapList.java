/* File:        $Id: License.txt,v $
 * Revision:    $Revision: 1.4 $
 * Author:      $Author: csr $
 * Date:        $Date: 2005/04/11 16:29:16 $
 *
 * Copyright Det Kongelige Bibliotek og Statsbiblioteket, Danmark
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

package dk.netarkivet.harvester.datamodel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;

/**
 * Class representing one or more global crawler traps, modelled as a list
 * of regular expressions. 
 *
 * @author csr
 * @since Nov 25, 2009
 */

public class GlobalCrawlerTrapList {

    /**
     * The unique id of this collection of crawler traps.
     */
    int id;

    /**
     * The list of traps. Each item is a regular expression matching url's to
     * be avoided. In the database, (id, trap) is a primary key so we model
     * the traps as a Set to avoid possible duplicates.
     */
    Set<String> traps;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * A unique name by which this list is identified
     */
    String name;

    /**
     * A free-text description of the traps in this collection.
     */
    String description;

    /**
     * Whether or not this set of traps is active (in use).
     */
    boolean isActive;

    /**
     * Protected constructor used by the DAO to create instances of this class.
     *
     * @param id  the id of this list.
     * @param name a name by which this list is known.
     * @param traps the set of trap expressions.
     * @param description A textual description of this list.
     * @param isActive flag indicating whether this list is isActive.
     * @throws ArgumentNotValid if the name is empty or null.
     */
    protected GlobalCrawlerTrapList(int id, List<String> traps, String name,
                                    String description, boolean isActive) throws
                                                             ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(name, "name");
        this.id = id;
        this.traps = new HashSet<String>(traps.size());
        this.traps.addAll(traps);
        this.description = description;
        this.isActive = isActive;
        this.name = name;
    }

    /**
     * Construct a new GlobalCrawlerTrapList from an input stream consisting of
     * newline-separated regular expressions.
     * @param is an input stream from which the list of trap expressions
     * can be read.
     * @param name a name by which this list is known.
     * @param description A textual description of this list.
     * @param isActive flag indicating whether this list is isActive.
     * @throws IOFailure if the input stream cannot be found or read.
     * @throws ArgumentNotValid if the input stream is null or the name is null or
     * empty.
     */
    public GlobalCrawlerTrapList(InputStream is, String name, String description,
                                 boolean isActive) throws IOFailure,
                                                          ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(name, "name");
        ArgumentNotValid.checkNotNull(is, "is");
        this.traps = new HashSet<String>();
        this.isActive = isActive;
        this.name = name;
        if (description == null) {
            this.description = "";
        } else {
            this.description = description;
        }
        setTrapsFromInputStream(is);
    }

    /**
     * A utility method to read the list of traps from an InputStream,
     * line-by-line
     * @param is  The input stream from which to read.
     */
    public void setTrapsFromInputStream(InputStream is) {
        traps.clear();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        try {
            while  ( (line = reader.readLine()) != null ) {
                  traps.add(line.trim());
            }
        } catch (IOException e) {
            throw new IOFailure("Could not read crawler traps", e);
        }
    }

    public int getId() {
        return id;
    }

    protected void setId(int id) {
        this.id = id;
    }

    public Set<String> getTraps() {
        return traps;
    }

    public void setTraps(Set<String> traps) {
        this.traps = traps;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GlobalCrawlerTrapList that = (GlobalCrawlerTrapList) o;

        if (id != that.id) {
            return false;
        }
        if (isActive != that.isActive) {
            return false;
        }
        if (description != null ? !description.equals(that.description)
                                : that.description != null) {
            return false;
        }
        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }
        if (traps != null ? !traps.equals(that.traps) : that.traps != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (traps != null ? traps.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode()
                                                    : 0);
        result = 31 * result + (isActive ? 1 : 0);
        return result;
    }
}