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

import javax.servlet.jsp.PageContext;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.DomainUtils;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.webinterface.EventHarvest;


/**
 * This class contains the specific properties and operations
 * of harvest definitions which are not snapshot harvest definitions.
 * I.e. this class models definitions of event and selective harvests.
 *
 */
public class PartialHarvest extends HarvestDefinition {
    private final Log log = LogFactory.getLog(getClass());

    /** Set of domain configurations being harvested by this harvest.
     * Entries in this set are unique on configuration name + domain name.
     */
    private Map<DomainConfigurationKey, DomainConfiguration> domainConfigurations
            = new HashMap<DomainConfigurationKey, DomainConfiguration>();

    /** The schedule used by this PartialHarvest. */
    private Schedule schedule;

    /**
     * The next date this harvest definition should run, null if never again.
     */
    private Date nextDate;

    /**
     * Create new instance of a PartialHavest configured according
     * to the properties of the supplied DomainConfiguration.
     *
     * @param domainConfigurations a list of domain configurations
     * @param schedule             the harvest definition schedule
     * @param harvestDefName       the name of the harvest definition
     * @param comments             comments
     */
    public PartialHarvest(List<DomainConfiguration> domainConfigurations,
                          Schedule schedule,
                          String harvestDefName,
                          String comments) {

        ArgumentNotValid.checkNotNull(schedule, "schedule");
        ScheduleDAO.getInstance().read(schedule.getName());

        ArgumentNotValid.checkNotNullOrEmpty(harvestDefName, "harvestDefName");
        ArgumentNotValid.checkNotNull(comments, "comments");
        ArgumentNotValid.checkNotNull(domainConfigurations,
                "domainConfigurations");

        this.numEvents = 0;
        addConfigurations(domainConfigurations);
        this.schedule = schedule;
        this.harvestDefName = harvestDefName;
        this.comments = comments;
        this.nextDate = schedule.getFirstEvent(new Date());
    }

    /**
     * Generates jobs in files from this harvest definition, and updates the
     * schedule for when the harvest definition should happen next time.
     *
     * Create Jobs from the domainconfigurations in this harvestdefinition
     * and the current value of the limits in Settings.
     * Multiple jobs are generated if different order.xml-templates are used,
     * or if the size of the job is inappropriate.
     *
     * The following settings are used:
     * {@link HarvesterSettings#JOBS_MAX_RELATIVE_SIZE_DIFFERENCE}:
     * The maximum relative difference between the smallest and largest
     * number of objects expected in a job
     * <p/>
     * {@link HarvesterSettings#JOBS_MIN_ABSOLUTE_SIZE_DIFFERENCE}:
     * Size differences below this threshold are ignored even if
     * the relative difference exceeds {@link HarvesterSettings#JOBS_MAX_RELATIVE_SIZE_DIFFERENCE}
     * <p/>
     * {@link HarvesterSettings#JOBS_MAX_TOTAL_JOBSIZE}:
     * The upper limit on the total number of objects that a job may
     * retrieve
     *
     * Also updates the harvest definition to schedule the next event using
     * the defined schedule. Will skip events if the next event would be in the
     * past when using the schedule definition.
     *
     * @return Number of jobs created
     */
    public int createJobs() {
        //Generate jobs
        int jobsMade = super.createJobs();

        //Calculate next event
        Date now = new Date();
        Date nextEvent = schedule.getNextEvent(getNextDate(), getNumEvents());

        //Refuse to schedule event in the past
        if (nextEvent != null && nextEvent.before(now)) {
            int eventsSkipped = 0;
            while (nextEvent != null && nextEvent.before(now)) {
                nextEvent = schedule.getNextEvent(nextEvent, getNumEvents());
                eventsSkipped++;
            }
            log.warn("Refusing to schedule harvest definition '"
                        + getName() + "' in the past. Skipped "
                        + eventsSkipped + " events. Old nextDate was "
                        + nextDate
                        + " new nextDate is " + nextEvent);
        }

        //Set next event
        setNextDate(nextEvent);
        log.trace("Next event for harvest definition " + getName()
                  + " happens: "
                  + (nextEvent == null ? "Never" : nextEvent.toString()));

        return jobsMade;
    }

    /**
     * Get a new Job suited for this type of HarvestDefinition.
     *
     * @param cfg The configuration to use when creating the job
     * @return a new job
     */
    protected Job getNewJob(DomainConfiguration cfg) {
        return Job.createJob(getOid(), cfg, numEvents);
    }

    /**
     * Returns the schedule defined for this harvest definition.
     *
     * @return schedule
     */
    public Schedule getSchedule() {
        return schedule;
    }

    /**
     * Set the schedule to be used for this harvestdefinition.
     *
     * @param schedule A schedule for when to try harvesting.
     */
    public void setSchedule(Schedule schedule) {
        ArgumentNotValid.checkNotNull(schedule, "schedule");
        this.schedule = schedule;
        if (nextDate != null) {
            setNextDate(schedule.getFirstEvent(nextDate));
        }
    }

    /**
     * Get the next date this harvest definition should be run.
     *
     * @return The next date the harvest definition should be run or null, if
     *         the harvest definition should never run again.
     */
    public Date getNextDate() {
        return nextDate;
    }

    /**
     * Set the next date this harvest definition should be run.
     *
     * @param nextDate The next date the harvest definition should be run.
     *                 May be null, meaning never again.
     */
    public void setNextDate(Date nextDate) {
        this.nextDate = nextDate;
    }

    /**
     * Remove domainconfiguration from this partialHarvest.
     * @param dcKey domainCondiguration key
     */
    public void removeDomainConfiguration(DomainConfigurationKey dcKey) {
        ArgumentNotValid.checkNotNull(dcKey, "DomainConfigurationKey dcKey");
        if (domainConfigurations.remove(dcKey) == null) {
            log.warn("Unable to delete domainConfiguration '" 
                    + dcKey + "' from " + this + ". Reason: didn't exist.");
        }
    }
    
    /** Add a new domainconfiguration to this PartialHarvest. 
     * @param newConfiguration A new DomainConfiguration
     */
    public void addDomainConfiguration(DomainConfiguration newConfiguration) {
        ArgumentNotValid.checkNotNull(newConfiguration, 
                "DomainConfiguration newConfiguration");
        DomainConfigurationKey key = new DomainConfigurationKey(
                newConfiguration);
        if (domainConfigurations.containsKey(key)) {
            log.warn("Unable to add domainConfiguration '" 
                    + newConfiguration + "' from " + this 
                    + ". Reason: does already exist.");
        } else {
            domainConfigurations.put(key, newConfiguration);
        }
    }
    
    
    /**
     * Returns a List of domain configurations for this harvest definition.
     *
     * @return List containing information about the domain configurations
     */
    public Iterator<DomainConfiguration> getDomainConfigurations() {
        return domainConfigurations.values().iterator();
    }

    /**
     * Set the list of configurations that this hd uses.
     *
     * @param configs List<DomainConfiguration> the configurations that this
     *                harvestdefinition will use.
     */
    public void setDomainConfigurations(List<DomainConfiguration> configs) {
        ArgumentNotValid.checkNotNull(configs, "configs");

        domainConfigurations.clear();
        addConfigurations(configs);
    }

    private void addConfigurations(List<DomainConfiguration> configs) {
        for (DomainConfiguration dc : configs) {
            addConfiguration(dc);
        }
    }

    private void addConfiguration(DomainConfiguration dc) {
        domainConfigurations.put(new DomainConfigurationKey(dc), dc);
    }

    /**
     * Reset the harvest definition to no harvests and next date being the
     * first possible for the schedule.
     */
    public void reset() {
        numEvents = 0;
        nextDate = schedule.getFirstEvent(new Date());
    }

    /**
     * Check if this harvest definition should be run, given the time now.
     *
     * @param now The current time
     * @return true if harvest definition should be run
     */
    public boolean runNow(Date now) {
        ArgumentNotValid.checkNotNull(now, "now");
        if (!getActive()) {
            return false; // inactive definitions are never run
        }
        return nextDate != null && now.compareTo(nextDate) >= 0;
    }

    /**
     * Returns whether this HarvestDefinition represents a snapshot harvest.
     *
     * @return false (always)
     */
    public boolean isSnapShot() {
        return false;
    }

    /**
     * Always returns no limit.
     * @return 0, meaning no limit.
     */
    protected long getMaxCountObjects() {
        return Constants.HERITRIX_MAXOBJECTS_INFINITY;
    }

    /** Always returns no limit.
     * @return -1, meaning no limit.
     */
    protected long getMaxBytes() {
        return Constants.HERITRIX_MAXBYTES_INFINITY;
    }

    /**
     * Takes a seed list and creates any necessary domains, configurations, and
     * seedlists to enable them to be harvested with the given template and
     *  other parameters.
     * <A href="https://sbforge.org/jira/browse/NAS-1317">JIRA issue NAS-1317</A>
     * addresses this issue.
     * Current naming of the seedlists and domainconfigurations are:
     *  one of <br>
     *  harvestdefinitionname + "_" + templateName + "_" + "UnlimitedBytes"
     *  (if maxbytes is negative)<br>
     *  harvestdefinitionname + "_" + templateName + "_" + maxBytes + "Bytes"
     *  (if maxbytes is zero or postive).
     * @see EventHarvest#addConfigurations(PageContext, I18n, String)
     * for details
     * @param seeds a newline-separated list of the seeds to be added
     * @param templateName the name of the template to be used
     * @param maxBytes Maximum number of bytes to harvest per domain
     * @param maxObjects Maximum number of objects to harvest per domain
     */
    public void addSeeds(String seeds, String templateName, long maxBytes,
                          int maxObjects) {
        ArgumentNotValid.checkNotNullOrEmpty(seeds, "seeds");
        ArgumentNotValid.checkNotNullOrEmpty(templateName, "templateName");
        if (!TemplateDAO.getInstance().exists(templateName)) {
            throw new UnknownID("No such template: " + templateName);
        }
        // Generate components for the name for the configuration and seedlist
        final String maxbytesSuffix = "Bytes";
        String maxBytesS = "Unlimited" + maxbytesSuffix;
        if (maxBytes >= 0) {
            maxBytesS = Long.toString(maxBytes);
            maxBytesS = maxBytesS + maxbytesSuffix;
        }

        final String maxobjectsSuffix = "Objects";
        String maxObjectsS = "Unlimited" + maxobjectsSuffix;
        if (maxObjects >= 0) {
            maxObjectsS = Long.toString(maxObjects);
            maxObjectsS = maxObjectsS + maxobjectsSuffix;
        }

        String name = harvestDefName + "_" + templateName + "_"
                      + maxBytesS+ "_" + maxObjectsS;

        // Note: Matches any sort of newline (unix/mac/dos), but won't get empty
        // lines, which is fine for this purpose
        System.out.println("Adding seeds: " + seeds);
        
        String[] seedArray = seeds.split("[\n\r]+");
        Map<String, Set<String>> acceptedSeeds
                = new HashMap<String, Set<String>>();
        StringBuilder invalidMessage =
                new StringBuilder("Unable to create an event harvest.\n"
                                  + "The following seeds are invalid:\n");
        boolean valid = true;
        //validate:

        for (String seed : seedArray) {
            seed = seed.trim();
            if (seed.length() != 0) {
                if (!(seed.startsWith("http://")
                      || seed.startsWith("https://"))) {
                    seed = "http://" + seed;
                }
                URL url = null;
                try {
                    url = new URL(seed);
                } catch (MalformedURLException e) {
                    valid = false;
                    invalidMessage.append(seed);
                    invalidMessage.append('\n');
                    continue;
                }
                String host = url.getHost();
                String domainName = DomainUtils.domainNameFromHostname(host);
                if (domainName == null) {
                    valid = false;
                    invalidMessage.append(seed);
                    invalidMessage.append('\n');
                    continue;
                }

                Set<String> seedsForDomain = acceptedSeeds.get(domainName);
                if (seedsForDomain == null) {
                    seedsForDomain = new HashSet<String>();
                }
                seedsForDomain.add(seed);
                acceptedSeeds.put(domainName, seedsForDomain);
            }
        }

        if (!valid) {
            throw new ArgumentNotValid(invalidMessage.toString());
        }

        for (Map.Entry<String, Set<String>> entry : acceptedSeeds.entrySet()) {
            String domainName = entry.getKey();
            Domain domain;

            // Need a seedlist to include in the configuration when we
            // create it. This will be replaced lated.
            SeedList seedlist = new SeedList(name, "");
            List<SeedList> seedListList = new ArrayList<SeedList>();
            seedListList.add(seedlist);

            //Find or create the domain
            if (DomainDAO.getInstance().exists(domainName)) {
                domain = DomainDAO.getInstance().read(domainName);
                if (!domain.hasSeedList(name)) {
                    domain.addSeedList(seedlist);
                }
            } else {
                domain = Domain.getDefaultDomain(domainName);
                domain.addSeedList(seedlist);
                DomainDAO.getInstance().create(domain);
            }
            //Find or create the DomainConfiguration
            DomainConfiguration dc = null;
            if (domain.hasConfiguration(name)) {
                dc = domain.getConfiguration(name);
            } else {
                dc = new DomainConfiguration(name, domain, seedListList,
                                             new ArrayList<Password>());
                dc.setOrderXmlName(templateName);

                dc.setMaxBytes(maxBytes);
                dc.setMaxObjects(maxObjects);
                domain.addConfiguration(dc);
            }

            //Find the SeedList and add this seed to it
            seedlist = domain.getSeedList(name);
            List<String> currentSeeds = seedlist.getSeeds();
            entry.getValue().addAll(currentSeeds);

            List<String> allSeeds = new ArrayList<String>();

            allSeeds.addAll(entry.getValue());
            domain.updateSeedList(new SeedList(name, allSeeds));


            //Add the configuration to the harvest config set.
            addConfiguration(dc);
            DomainDAO.getInstance().update(domain);
        }

        HarvestDefinition thisInDAO =
                HarvestDefinitionDAO.getInstance().
                        getHarvestDefinition(this.harvestDefName);
        if (thisInDAO == null) {
            HarvestDefinitionDAO.getInstance().create(this);
        } else {
            HarvestDefinitionDAO.getInstance().update(this);
        }
    }

}
