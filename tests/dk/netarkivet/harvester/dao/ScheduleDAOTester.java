/* $Id$
 * $Revision$
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
package dk.netarkivet.harvester.dao;


import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.harvester.dao.ScheduleDAO;
import dk.netarkivet.harvester.datamodel.DataModelTestCase;
import dk.netarkivet.harvester.datamodel.RepeatingSchedule;
import dk.netarkivet.harvester.datamodel.Schedule;
import dk.netarkivet.harvester.datamodel.TestInfo;
import dk.netarkivet.harvester.datamodel.TimedSchedule;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class ScheduleDAOTester extends DataModelTestCase {
    public ScheduleDAOTester(String sTestName) {
        super(sTestName);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Verify that the standard usage works:
     * Get an instance of a ScheduleDAO
     * create a new schedule and retrieve the stored data from it
     * @param schedule A schedule to test.
     */
    public void doTestNormalUsage(final Schedule schedule) {
        ScheduleDAO scheduledao = ScheduleDAO.getInstance();
        assertNotNull("Expected valid dao", scheduledao);
        scheduledao.create(schedule);

        // now verify that it is possible to retrieve stored information
        ScheduleDAO newscheduledao = ScheduleDAO.getInstance();
        Schedule readschedule = newscheduledao.read(schedule.getName());
        assertNotNull("Previously stored schedule expected", readschedule);
        assertEquals("Read data must match stored data",
                schedule.getName(),
                readschedule.getName());
        assertEquals("Read data must match stored data",
                schedule.getComments(),
                readschedule.getComments());
        assertEquals("Read data must match stored data",
                schedule.getStartDate(),
                readschedule.getStartDate());
        if (schedule instanceof RepeatingSchedule) {
            assertTrue("Read schedule must be repeating: " + readschedule,
                    readschedule instanceof RepeatingSchedule);
            assertEquals("Number of repeats must be the same",
                    ((RepeatingSchedule) schedule).getRepeats(),
                    ((RepeatingSchedule) readschedule).getRepeats());
        } else {
            assertTrue("Read schedule must be timed: " + readschedule,
                    readschedule instanceof TimedSchedule);
            assertEquals("Read data must match stored data",
                    ((TimedSchedule) schedule).getEndDate(),
                    ((TimedSchedule) readschedule).getEndDate());
        }
        assertEquals("Read data must match stored data",
                schedule.getFrequency(),
                readschedule.getFrequency());
    }

    /** Test an hourly schedule. */
    public void testNormalUsageHourly() {
        doTestNormalUsage(TestInfo.TESTSCHEDULE_HOURLY);
    }

    /** Test a daily schedule. */
    public void testNormalUsageDaily() {
        doTestNormalUsage(TestInfo.TESTSCHEDULE_DAILY);
    }

    /** Test a weekly schedule. */
    public void testNormalUsageWeekly() {
        doTestNormalUsage(TestInfo.TESTSCHEDULE_WEEKLY);
    }

    /** Test a monthly schedule. */
    public void testNormalUsageMonthly() {
        doTestNormalUsage(TestInfo.TESTSCHEDULE_MONTHLY);
    }

    /**
     * Verify that basic exceptions works:
     * Get an instance of a ScheduleDAO
     * create a null schedule and retrieve null data from it
     */
    public void testBasicArgumentExceptions() {
        ScheduleDAO scheduledao = ScheduleDAO.getInstance();
        /** verify that create with null throws an exception */
        try {
            scheduledao.create(null);
            fail("null is not an allowed argument to create.");
        } catch (ArgumentNotValid e) {
            //expected
        }

        /** verify that create with null throws an exception */
        try {
            Schedule sch = TestInfo.getDefaultSchedule();
            scheduledao.create(sch);
            fail("Not allowed to create multiple schedules with identical names.");
        } catch (PermissionDenied e) {
            //expected
        }

        /** verify that read with null throws an exception */
        try {
            scheduledao.read(null);
            fail("null is not an allowed argument to read.");
        } catch (ArgumentNotValid e) {
            //expected
        }
    }


    /**
     * Test retrieval of all schedules
     * DEFAULTSCHEDULE is already there in the schedules list.
     * add one HERE_AND_NOW_SCHEDULE schedule more and
     * retrieve and count all schedules
     */
    public void testGetAllSchedules() {
        ScheduleDAO scheduledao = ScheduleDAO.getInstance();

        // Retrieve all schedules and count schedules already present = 2

        int original_count = 0;
        Iterator<Schedule>  original_slw = scheduledao.getAllSchedules();
        List<Schedule> original_schedules = new ArrayList<Schedule> ();
        while (original_slw.hasNext()) {
            original_schedules.add(original_slw.next());
            ++original_count;
        }

        // Add one more new schedule
        scheduledao.create(TestInfo.HERE_AND_NOW_SCHEDULE);

        int count = 0;
        Iterator<Schedule> slw = scheduledao.getAllSchedules();
        List<Schedule> schedules = new ArrayList<Schedule>();
        while (slw.hasNext()) {
            schedules.add(slw.next());
            ++count;
        }
        // not the most stringent test but probably sufficient
        int added_schedules = count - original_count;
        assertEquals("1 schedule added, 1 schedule expected, but got "
                + schedules, 1, added_schedules);
    }

    /** Check that updating an entry that has already been modified
     *  results in an IOFailure
     *  */
    public void testOptimisticLocking() {
        // create the schedule
        Schedule schedule = TestInfo.TESTSCHEDULE_HOURLY;
        ScheduleDAO scheduledao = ScheduleDAO.getInstance();
        assertNotNull("Expected valid dao", scheduledao);
        scheduledao.create(schedule);

        // retrieve two instances of the schedule
        Schedule readschedule1 = scheduledao.read(schedule.getName());
        Schedule readschedule2 = scheduledao.read(schedule.getName());

        // Updating the first schedule should succeed and increment the
        // edition number, even though no changes were made to the schedule
        scheduledao.update(readschedule1);

        try {
          scheduledao.update(readschedule2);
          fail("The edition of readschedule expired when readschedule1 was updated");
        } catch (PermissionDenied e) {
          //expected
        }

    }

    /** Reset the DAO instance */
    public static void resetDAO() {
        ScheduleDAO.reset();
    }
}
