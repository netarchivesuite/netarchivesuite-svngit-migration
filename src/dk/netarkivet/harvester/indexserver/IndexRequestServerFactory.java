/* $Id$
 * $Revision$
 * $Date$
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *   USA
 */
package dk.netarkivet.harvester.indexserver;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.SettingsFactory;
import dk.netarkivet.harvester.HarvesterSettings;

public class IndexRequestServerFactory extends SettingsFactory<IndexRequestServerInterface> {

        /**
         * Returns an instance of the chosen IndexRequestServerInterface 
         * implementation defined by the setting
         * settings.archive.indexserver.indexrequestserver.class .
         * This class must have a getInstance method
         * @throws ArgumentNotValid if the instance cannot be constructed.
         * @return an IndexRequestServerInterface instance. 
         */
        public static IndexRequestServerInterface getInstance() throws ArgumentNotValid {
            return SettingsFactory.getInstance(
                    HarvesterSettings.INDEXREQUEST_SERVER_CLASS);
        }
}
