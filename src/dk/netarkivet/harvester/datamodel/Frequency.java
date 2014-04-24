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

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.NotImplementedException;

/**
 * This class defines various frequencies at which things can happen, such
 * as midnight every day, 13:45 the first monday of a month, etc.
 *
 */

public abstract class Frequency {
    
    private static final Log log = LogFactory.getLog(Frequency.class);
    
    /** How many units of time between each event? */
    private int numUnits;

    /** If this Frequency happens any time rather than at a specified time. */
    private boolean isAnytime;

    /** Initialise a frequency with information about how many periods 
     * between events, and whether it's at a specified time in the period.
     *
     * The actual length of the period is defined by subclasses
     *
     * @param numUnits Number of periods between events
     * @param isAnytime Whether it's at a specified time in the period
     * @throws ArgumentNotValid if numUnits if 0 or negative
     */
    public Frequency(int numUnits, boolean isAnytime) {
        ArgumentNotValid.checkPositive(numUnits, "numUnits");

        this.numUnits = numUnits;
        this.isAnytime = isAnytime;
    }

    /** Given when the last event happened, tell us when the next event should
     * happen (even if the new event is in the past).
     *
     * The time of the next event is guaranteed to be later that lastEvent.
     * For certain frequencies (e.g. once a day, any time of day), the time
     * of the next event is derived from lastEvent, for others (e.g. once a day
     * at 13:00) the time of the next event is the first matching time after
     * lastEvent.
     *
     * These methods are used by the schedule methods for calculating when
     * events should happen.
     *
     * @param lastEvent A time from which the next event should be calculated.
     * @return At what point the event should happen next.
     */
    public abstract Date getNextEvent(Date lastEvent);

    /** Given a starting time, tell us when the first event should happen.
     *
     * This method is used by the schedule methods for calculating when events
     * should happen.
     *
     * @param startTime The earliest time the event can happen.
     * @return At what point the event should happen the first time.
     */
    public abstract Date getFirstEvent(Date startTime);

    /** Returns the number of periods between events.
     *
     * @return that number
     */
    public int getNumUnits() {
        return numUnits;
    }

    /** Returns whether this frequency allows events to happen any time of day,
     * rather than at a specific time.
     * @return true if the events may happen at any time.
     */
    public boolean isAnytime() {
        return isAnytime;
    }

    /**
     * Autogenerated equals.
     * @param o The object to compare with
     * @return Whether objects are equal
     */
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Frequency)) return false;

        final Frequency frequency = (Frequency) o;

        if (isAnytime != frequency.isAnytime) return false;
        if (numUnits != frequency.numUnits) return false;

        return true;
    }

    /**
     * Autogenerated hashcode method.
     * @return the hashcode
     */
    public int hashCode() {
        int result;
        result = numUnits;
        result = 29 * result + (isAnytime ? 1 : 0);
        return result;
    }

    /** Return the exact minute event should happen on, or null if this is
     * an anyTime event or doesn't define what minute it should happen on.
     * @return the exact minute event should happen on
     */
    public abstract Integer getOnMinute();

    /** Return the exact hour event should happen on, or null if this is
     * an anyTime event or doesn't define what hour it should happen on.
     * @return the exact hour event should happen on
     */
    public abstract Integer getOnHour();

    /** Return the exact day of week event should happen on, or null if this is
     * an anyTime event or doesn't define what day of week it should happen on.
     * @return the exact day of week event should happen on
     */
    public abstract Integer getOnDayOfWeek();

    /** Return the exact day of month event should happen on, or null if this is
     * an anyTime event or doesn't define what day of month it should happen on.
     * @return the exact day of month event should happen on
     */
    public abstract Integer getOnDayOfMonth();

    /** Return an integer that can be used to identify the kind of frequency.
     * No two subclasses should use the same integer
     * @return Return an integer that can be used to identify
     *  the kind of frequency
     */
    public abstract int ordinal();
    
    /**
     * Get a new instance of Frequency. The type of Frequency (either Hourly,
     * Daily, Monthly, or Weekly) returned depends on the 'timeunit' argument.
     * @param timeunit The type or frequency
     * @param anytime Allow events to start anytime. If false,
     * the starting point is described precisely.
     * If true, the starting point will be immediately.
     * @param numtimeunits The number of periods between events
     * @param minute A given minute. Used to create hourly, daily, and monthly frequencies,
     * if anytime is false.
     * @param hour A given hour. Used to create hourly, daily, and monthly frequencies,
     * if anytime is false.
     * @param dayofweek A given day of the week. Used only to create weekly frequencies,
     * if anytime is false.
     * @param dayofmonth A given day of month. Used only to create monthly frequencies,
     * if anytime is false.
     * @return a new instance of the Frequency class.
     * @throws ArgumentNotValid If the given timeunit is illegal,
     * or the values of timeunit and numtimeunits is negative.
     * @throws NotImplementedException If we can't yet make a
     * Frequency for a legal timeunit. 
     */
    public static Frequency getNewInstance(int timeunit, boolean anytime,
                                               int numtimeunits,
                                               Integer minute, Integer hour,
                                               Integer dayofweek,
                                               Integer dayofmonth) {
        ArgumentNotValid.checkPositive(timeunit, "int timeunit");
        ArgumentNotValid.checkPositive(numtimeunits, "int timeunits");
                
        Frequency freq;
        TimeUnit tu = TimeUnit.fromOrdinal(timeunit);
        log.debug("Creating a " + tu.name() + " frequency."); 
        if (!anytime) {
            ArgumentNotValid.checkTrue(minute != null,
                    "Arg. minute should not be null, if anytime is false");
            ArgumentNotValid.checkTrue(hour != null || tu.equals(TimeUnit.HOURLY),
                    "Arg. hour should not be null, if anytime is false unless"
                    + " we are creating a Hourly frequency.");
            ArgumentNotValid.checkTrue(dayofweek != null || !tu.equals(TimeUnit.WEEKLY),
                    "Arg. dayofweek should not be null, if anytime is false "
                    + " and we are creating a Weekly frequency.");
            ArgumentNotValid.checkTrue(dayofmonth != null || !tu.equals(TimeUnit.MONTHLY),
                    "Arg. dayofmonth should not be null, if anytime is false "
                    + "and we are creating a Monthly frequency.");
        }
    
        switch (tu) {
            case HOURLY:
                if (anytime) {
                    freq = new HourlyFrequency(numtimeunits);
                } else {
                    freq = new HourlyFrequency(numtimeunits, minute);
                }
                break;
            case DAILY:
                if (anytime) {
                    freq = new DailyFrequency(numtimeunits);
                } else {
                    freq = new DailyFrequency(numtimeunits, hour, minute);
                }
                break;
            case WEEKLY:
                if (anytime) {
                    freq = new WeeklyFrequency(numtimeunits);
                } else {
                    freq = new WeeklyFrequency(numtimeunits, dayofweek,
                            hour, minute);
                }
                break;
            case MONTHLY:
                if (anytime) {
                    freq = new MonthlyFrequency(numtimeunits);
                } else {
                    freq = new MonthlyFrequency(numtimeunits, dayofmonth,
                            hour, minute);
                }
                break;
            case MINUTE:   //Minute frequencies are always "anytime"
                freq = new MinuteFrequency(numtimeunits);
                break;
            default:
                throw new NotImplementedException(
                        "We don't know how to make a Frequency for timeunit "
                        + timeunit);
        }
        return freq;
    }
}
