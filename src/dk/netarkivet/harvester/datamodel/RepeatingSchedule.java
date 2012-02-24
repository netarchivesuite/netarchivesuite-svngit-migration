/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

import dk.netarkivet.common.exceptions.ArgumentNotValid;

import java.util.Date;

/**
 * This class implements a schedule that should run a certain number of times.
 *
 */

public class RepeatingSchedule extends Schedule {
    /** How many times this schedule should be repeated.*/
    private final int repeats;

    /** Create a new RepeatingSchedule that runs a given number of times.
     *
     * @param startDate The time at which the schedule starts running.  This
     * is not necessarily the time of the first event, but no events will
     * happen before this. May be null, meaning start any time.
     * @param repeats how many events should happen totally.
     * @param frequency How frequently the event should happen.
     * @param name The unique name of the schedule.
     * @param comments Comments entered by the user
     * @throws ArgumentNotValid if frequency, name or comments is null, or name
     * is "" or repeats is 0 or negative
     */
    public RepeatingSchedule(Date startDate, int repeats, Frequency frequency,
                             String name, String comments) {
        super(startDate, frequency, name, comments);
        ArgumentNotValid.checkPositive(repeats, "repeats");

        this.repeats = repeats;
    }

    /**
     * Autogenerated equals.
     * @param o The object to compare with
     * @return Whether objects are equal
     */
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RepeatingSchedule)) return false;
        if (!super.equals(o)) return false;

        final RepeatingSchedule repeatingSchedule = (RepeatingSchedule) o;

        if (repeats != repeatingSchedule.repeats) return false;

        return true;
    }

    /**
     * Autogenerated hashcode method.
     * @return the hashcode
     */
    public int hashCode() {
        int result = super.hashCode();
        result = 29 * result + repeats;
        return result;
    }

    /**
     * Return the date at which the next event will happen.
     *
     * @param lastEvent          The time at which the previous event happened.
     * @param numPreviousEvents How many events have previously happened.
     * @return The date of the next event to happen or null for no more events.
     * @throws ArgumentNotValid if numPreviousEvents is negative
     */
    public Date getNextEvent(Date lastEvent, int numPreviousEvents) {
        ArgumentNotValid.checkNotNegative(numPreviousEvents,
                                          "numPreviousEvents");

        if (lastEvent == null) {
            return null;
        }
        if (numPreviousEvents >= repeats) {
            return null;
        }
        return frequency.getNextEvent(lastEvent);
    }

    /** Return how many times this schedule should be triggered.
     * @return That number of times
     */
    public int getRepeats() {
        return repeats;
    }

    /** Human readable represenation of this object.
     *
     * @return Human readble representation
     */
    public String toString() {
        return name + ": " + repeats + " times "
               + frequency + "(" + comments + ")";
    }
}
