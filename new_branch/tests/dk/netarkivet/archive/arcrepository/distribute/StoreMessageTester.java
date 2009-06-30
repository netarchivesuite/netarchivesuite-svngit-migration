/*$Id$
* $Revision$
* $Date$
* $Author$
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
package dk.netarkivet.archive.arcrepository.distribute;

import java.io.File;

import junit.framework.TestCase;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnectionTestMQ;
import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 */
public class StoreMessageTester extends TestCase {

    public void setUp() {
        JMSConnectionTestMQ.useJMSConnectionTestMQ();
    }

    public void tearDown() {
        Settings.reload();
    }

    public void testInvalidArguments() {

        try {
            new StoreMessage(null, new File(""));
            fail("Should throw ArgumentNotValid on null Channel");
        } catch (ArgumentNotValid e) {
            // expected case
        }

        try {
            new StoreMessage(Channels.getTheArcrepos(), null);
            fail("Should throw ArgumentNotValid on null file");
        } catch (ArgumentNotValid e) {
            // expected case
        }
    }

}