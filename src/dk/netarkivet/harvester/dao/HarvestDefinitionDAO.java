/* File:        $Id: HarvestDefinitionDAO.java 2803 2013-10-29 15:42:35Z ngiraud $
 * Revision:    $Revision: 2803 $
 * Author:      $Author: ngiraud $
 * Date:        $Date: 2013-10-29 16:42:35 +0100 (Tue, 29 Oct 2013) $
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
package dk.netarkivet.harvester.dao;


import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.harvester.datamodel.DomainConfiguration;
import dk.netarkivet.harvester.datamodel.HarvestChannel;
import dk.netarkivet.harvester.datamodel.HarvestDefinition;
import dk.netarkivet.harvester.datamodel.HarvestRunInfo;
import dk.netarkivet.harvester.datamodel.PartialHarvest;
import dk.netarkivet.harvester.datamodel.SparseDomainConfiguration;
import dk.netarkivet.harvester.datamodel.SparseFullHarvest;
import dk.netarkivet.harvester.datamodel.SparsePartialHarvest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A Data Access Object for harvest definitions.
 * This object is a singleton to ensure thread-safety. It
 * handles the transformation from harvest definitions to persistent storage.
 *
 */
public abstract class HarvestDefinitionDAO
extends HarvestDatabaseDAO
implements Iterable<HarvestDefinition> {

    /** The one and only instance of the HarvestDefinitionDAO class to ensure
      * thread-safety.
      */
    private static HarvestDefinitionDAO instance;

    /** The log. */
    protected final Log log = LogFactory.getLog(getClass());

    /**
     * Default constructor.
     * Does not do anything, however.
     */
    protected HarvestDefinitionDAO() {
    }

    /**
     * Creates the singleton.
     * @return the HarvestDefinitionDAO singleton.
     * @throws IOFailure if unable to create the singleton.
     */
    public static synchronized HarvestDefinitionDAO getInstance() {
        if (instance == null) {
            instance = new HarvestDefinitionDBDAO();
        }
        return instance;
    }

    /**
     * Create a harvest definition in persistent storage.
     *
     * @param harvestDefinition A new harvest definition to write out.
     * @return The harvestId for the just created harvest definition.
     */
    public abstract Long create(final HarvestDefinition harvestDefinition);

    /**
     * Read the stored harvest definition for the given ID.
     *
     * @param harvestDefinitionID An ID number for a harvest definition
     * @return A harvest definition that has been read from persistent storage.
     * @throws UnknownID if no file with that ID exists
     * @throws IOFailure if the File does not exist, does not have the
     *                   correct ID, or
     *                   otherwise fails to load correctly.
     */
    public abstract HarvestDefinition read(final Long harvestDefinitionID)
            throws UnknownID, IOFailure;

    /**
     * Update an existing harvest definition with new info
     * in persistent storage.
     *
     * @param harvestDefinition An updated harvest definition
     *  object to be persisted.
     */
    public abstract void update(final HarvestDefinition harvestDefinition);

    /**
     * Activates or deactivates a partial harvest definition,
     * depending on its activation status.
     * @param harvestDefinition the harvest definition object
     */
    public abstract void flipActive(final SparsePartialHarvest harvestDefinition);

    /**
     * Check, if there exists a HarvestDefinition identified by a given OID.
     * @param oid a given OID
     * @return true, if such a harvestdefinition exists.
     */
    public abstract boolean exists(final Long oid);

    /**
     * Check, if there exists a HarvestDefinition identified by a given name.
     * @param name a given name
     * @return true, if such a harvestdefinition exists.
     */
    public abstract boolean exists(final String name);

    /**
     * Get a list of all existing harvest definitions.
     *
     * @return An iterator that give the existing harvest definitions in turn
     */
    public abstract Iterator<HarvestDefinition> getAllHarvestDefinitions();

    /** Get an iterator of all harvest definitions.
     * Implements the Iterable interface.
     *
     * @return Iterator of all harvest definitions, Selective and Full both.
     */
    public Iterator<HarvestDefinition> iterator() {
        return getAllHarvestDefinitions();
    }

    /**
     * Gets default configurations for all domains.
     *
     * @return Iterator containing the default DomainConfiguration
     * for all domains
     */
    public abstract Iterator<DomainConfiguration> getSnapShotConfigurations();

    /** Get the IDs of the harvest definitions that are ready to run.
     *
     * @param now
     * @return IDs of the harvest definitions that are currently ready to
     * be scheduled.  Some of these might already be in the process of being
     * scheduled.
     */
    public abstract Iterable<Long> getReadyHarvestDefinitions(final Date now);

    /**
     * Get the harvest definition that has the given name, or null,
     * if no harvestdefinition exist with this name.
     * @param name The name of a harvest definition.
     * @return The HarvestDefinition object with that name, or null if none
     * has that name.
     */
    public abstract HarvestDefinition getHarvestDefinition(final String name);

    /** Returns a list with information on the runs of a particular harvest.
     * The list is ordered by descending run number.
     *
     * @param harvestID ID of an existing harvest
     * @return List of objects with selected information.
     */
    public abstract List<HarvestRunInfo> getHarvestRunInfo(final long harvestID);

    /**
     * Reset the DAO instance. Only for use in tests.
     */
    public static void reset() {
        instance = null;
    }

    /**
     * Get all domain,configuration pairs for a harvest definition in sparse
     * version for GUI purposes.
     *
     * @param harvestDefinitionID The ID of the harvest definition.
     * @return Domain,configuration pairs for that HD. Returns an empty list
     *         for unknown harvest definitions.
     * @throws ArgumentNotValid on null argument.
     */
    public abstract List<SparseDomainConfiguration>
            getSparseDomainConfigurations(final Long harvestDefinitionID);

    /**
     * Get a sparse version of a partial harvest for GUI purposes.
     *
     * @param harvestName Name of harvest definition.
     * @return Sparse version of partial harvest or null for none.
     * @throws ArgumentNotValid on null or empty name.
     */
    public abstract SparsePartialHarvest getSparsePartialHarvest(
    		final String harvestName);

    /**
     * Get all sparse versions of partial harvests for GUI purposes.
     *
     * @param excludeInactive If true only active harvest definitions are
     * returned.
     * @return An iterable (possibly empty) of SparsePartialHarvests
     */
    public abstract Iterable<SparsePartialHarvest>
           getSparsePartialHarvestDefinitions(final boolean excludeInactive);

    /**
     * Get a sparse version of a full harvest for GUI purposes.
     *
     * @param harvestName Name of harvest definition.
     * @return Sparse version of full harvest or null for none.
     * @throws ArgumentNotValid on null or empty name.
     */
    public abstract SparseFullHarvest getSparseFullHarvest(final String harvestName);

    /**
     * Get all sparse versions of full harvests for GUI purposes.
     *
     * @return An iterable (possibly empty) of SparseFullHarvests
     */
    public abstract Iterable<SparseFullHarvest>
            getAllSparseFullHarvestDefinitions();

    /** Get the name of a harvest given its ID.
     *
     * @param harvestDefinitionID The ID of a harvest
     * @return The name of the given harvest.
     * @throws ArgumentNotValid on null argument
     * @throws UnknownID if no harvest has the given ID.
     * @throws IOFailure        on any other error talking to the database
     */
    public abstract String getHarvestName(final Long harvestDefinitionID);

    /** Get whether a given harvest is a snapshot or selective harvest.
     *
     * @param harvestDefinitionID ID of a harvest
     * @return True if the given harvest is a snapshot harvest, false
     * otherwise.
     * @throws ArgumentNotValid on null argument
     * @throws UnknownID if no harvest has the given ID.
     * @throws IOFailure        on any other error talking to the database
     */
    public abstract boolean isSnapshot(final Long harvestDefinitionID);

    /** Get a sorted list of all domainnames of a HarvestDefintion
    *
    * @param harvestName of HarvestDefintion
    * @return List of all domains of the HarvestDefinition.
    * @throws ArgumentNotValid on null argument
    * @throws IOFailure        on any other error talking to the database
    */
    public abstract List<String> getListOfDomainsOfHarvestDefinition(
    		final String harvestName);

    /** Get a sorted list of all seeds of a Domain in a HarvestDefinition.
    *
    * @param harvestName of HarvestDefintion
    * @param domainName of Domain
    * @return List of all seeds of the Domain in the HarvestDefinition.
    * @throws ArgumentNotValid on null argument
    * @throws IOFailure        on any other error talking to the database
    */
    public abstract List<String> getListOfSeedsOfDomainOfHarvestDefinition(
    		final String harvestName, final String domainName);

    /**
     * Get a collection of jobIds for snapshot deduplication index.
     * @param harvestId the id of the harvest
     * @return a collection of jobIds to create a deduplication index.
     */
    public abstract Set<Long> getJobIdsForSnapshotDeduplicationIndex(
    		final Long harvestId);

    /**
     * Set the isindexready field available for snapshot harvests.
     * @param harvestId the ID of the harvest.
     * @param newValue the new isindexready value
     */
    public abstract void setIndexIsReady(final Long harvestId, final boolean newValue);

    /**
     * Remove Domain configuration from a specific PartialHarvest.
     * @param harvestId Id for a specific PartialHarvest
     * @param key a SparseDomainConfiguration uniquely identifying the
     * domainconfig.
     */
    public abstract void removeDomainConfiguration(
    		final Long harvestId,
    		final SparseDomainConfiguration key);
    /**
     * Update the given PartialHarvest (i.e. Selective Harvest) with a new
     * time for the next harvestrun. If no selective harvest matching the given id is
     * found in the storage, the method should silently return.
     * @param harvestId A given PartialHarvest id (i.e. Selective Harvest).
     * @param nextdate A new date for the next harvest run.
     */
    public abstract void updateNextdate(final long harvestId, final Date nextdate);

    /**
     * Add a domainconfiguration to a PartialHarvest.
     * @param hdd a given PartialHarvest
     * @param sparseDomainConfiguration a reduced domainconfiguration object
     */
    public abstract void addDomainConfiguration(
    		final PartialHarvest hdd,
    		final SparseDomainConfiguration sparseDomainConfiguration);
    
    /**
     * Maps a harvest definition to a harvest channel.
     * @param harvestDefinitionId the harvest definition id
     * @param channel the harvest channel
     */
    public abstract void mapToHarvestChannel(
    		final long harvestDefinitionId, 
    		final HarvestChannel channel);
    
}
