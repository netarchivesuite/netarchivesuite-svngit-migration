/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.wayback.indexer;

import junit.framework.TestCase;
import org.hibernate.Session;

public class HibernateUtilTester extends TestCase {

    /**
     * Tests that we can create an open session.
     */
    public void testGetSession() {
        Session session = HibernateUtil.getSession();
        assertTrue("Session should be connected.", session.isConnected());
        assertTrue("Session should be open.", session.isOpen());
        assertFalse("Session should not be dirty.", session.isDirty());
    }

}
