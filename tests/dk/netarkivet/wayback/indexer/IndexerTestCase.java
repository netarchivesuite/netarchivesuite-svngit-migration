/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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

import java.io.File;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.LocalArcRepositoryClient;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.wayback.TestInfo;
import dk.netarkivet.wayback.WaybackSettings;

import junit.framework.TestCase;

public class IndexerTestCase extends TestCase {
    private String oldClient = System.getProperty(CommonSettings.ARC_REPOSITORY_CLIENT);
    private String oldFileDir = System.getProperty("settings.common.arcrepositoryClient.fileDir");
    protected static File tempdir = new File(Settings.get(WaybackSettings.WAYBACK_INDEX_TEMPDIR));

    ReloadSettings rs = new ReloadSettings();

    public void setUp() {
        rs.setUp();
        System.setProperty(WaybackSettings.HIBERNATE_HBM2DDL_AUTO, "create-drop");
        HibernateUtil.getSession().getSessionFactory().close();
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR, TestInfo.WORKING_DIR);
        System.setProperty(CommonSettings.ARC_REPOSITORY_CLIENT, "dk.netarkivet.common.distribute.arcrepository.LocalArcRepositoryClient");
        System.setProperty("settings.common.arcrepositoryClient.fileDir", TestInfo.FILE_DIR.getAbsolutePath());
        System.setProperty(CommonSettings.REMOTE_FILE_CLASS, "dk.netarkivet.common.distribute.TestRemoteFile");
        assertTrue(ArcRepositoryClientFactory.getPreservationInstance() instanceof LocalArcRepositoryClient);
    }

    public void tearDown() {
        HibernateUtil.getSession().getSessionFactory().close();
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        FileUtils.remove(TestInfo.LOG_FILE);
        if (oldClient != null) {
            System.setProperty(CommonSettings.ARC_REPOSITORY_CLIENT, oldClient);
        } else {
            System.setProperty(CommonSettings.ARC_REPOSITORY_CLIENT, "");
        }
        if (oldFileDir != null ) {
            System.setProperty("settings.common.arcrepositoryClient.fileDir", oldFileDir);
        } else {
            System.setProperty("settings.common.arcrepositoryClient.fileDir", "");
        }
        rs.tearDown();
    }

    public void testNothing() {
        assertTrue("This is here to stop junit complaining that there are no "
                   + "tests in this class", true);
    }
}
