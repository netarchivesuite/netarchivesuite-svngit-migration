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

package dk.netarkivet.harvester.harvesting.controller;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.SettingsFactory;
import dk.netarkivet.harvester.HarvesterSettings;

/**
 *  A factory class for HeritrixController instances.
 */
public class HeritrixControllerFactory
        extends SettingsFactory<HeritrixController> {

    /**
     * Returns an instance of the default HeritrixController implementation
     * defined by the setting
     * dk.netarkivet.harvester.harvesting.heritrixController.class .
     * This class must have a constructor or factory method with a
     * signature matching the array args.
     * @param args the arguments to the constructor or factory method
     * @throws ArgumentNotValid if the instance cannot be constructed.
     * @return the HeritrixController instance.
     */
    public static HeritrixController
    getDefaultHeritrixController(Object ...args) throws ArgumentNotValid {
        return SettingsFactory.getInstance(
                HarvesterSettings.HERITRIX_CONTROLLER_CLASS, args);
    }
}
