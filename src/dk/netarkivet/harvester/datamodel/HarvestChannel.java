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

import java.io.Serializable;

import javax.servlet.jsp.PageContext;

import com.sun.org.apache.xalan.internal.xsltc.compiler.Pattern;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.harvester.harvesting.HarvestController;

/**
 * Harvest channels are used to dispatch harvest jobs to specific pools of crawlers. 
 * Channels can accept either only snapshot jobs or only focused jobs. Snapshot crawls all
 * use a single hard-coded channel.
 *
 * Harvest channels names must only contain alphanumeric characters, the constraint 
 * is enforced at creation time.
 *
 * {@link HarvestDefinition}s are mapped to a {@link HarvestChannel}, 
 * and {@link HarvestController}s listen to jobs sent on a specific channel.
 *
 * Harvest channels are stored in the harvest database, as well as mappings to 
 * {@link HarvestDefinition}s and {@link HarvestController}s through two association
 * tables.
 *
 * There must be exactly one channel defined as default for every type of 
 * job (snapshot and focused). This constraint will be enforced by the DAO. 
 *
 * @author ngiraud
 *
 */
@SuppressWarnings("serial")
public class HarvestChannel implements Serializable {

	/**
	 * The unique {@link dk.netarkivet.harvester.datamodel.HarvestChannel} for snapshot harvests.
	 */
	//public static final HarvestChannel SNAPSHOT = new HarvestChannel();

	/**
	 * Defines acceptable channel names: at least one word character (see {@link Pattern}).
	 */
	public static final String ACCEPTABLE_NAME_PATTERN = "^\\w+$";

	/** The unique numeric id. */
	private long id;

	/**
	 * The unique name of the channel. Accepts only alpha numeric characters.
	 * @see #ACCEPTABLE_NAME_PATTERN
	 * @see #isAcceptableName(String)
	 */
	private String name;

	/* Whether this channels type is snapshot or focused. */
	private boolean isSnapshot;

	/** Whether this channel is the default one for the given type (snapshot or focused). */
	private boolean isDefault;

	/** Comments.
	 */
	private String comments;

	/**
	 * Used to build the {@link #SNAPSHOT} singleton only.
	 */
	/*
	private HarvestChannel() {
		this.name = Settings.get(HarvesterSettings.SNAPSHOT_HARVEST_CHANNEL_ID);
		this.isSnapshot = true;
		this.isDefault = true;
		this.comments = "";
	}
	*/

	/**
	 * Constructor from name and comments
	 * @param name channel name
	 * @param isSnapshot whether this channels type is snapshot or focused
	 * @param isDefault whether this channel is the default one
	 * @param comments user comments
	 * (snapshot or focused)
	 * @throws ArgumentNotValid if the name is incorrect.
	 */
	public HarvestChannel(String name, boolean isSnapshot, boolean isDefault, String comments) {
		if (!isAcceptableName(name)) {
			throw new ArgumentNotValid("'" + name + "' does not match pattern '"
					+ ACCEPTABLE_NAME_PATTERN + "'");
		}
		this.name = name;
		this.isSnapshot = isSnapshot;
		this.isDefault = isDefault;
		this.comments = comments;
	}

	/**
	 * Constructor from persistent storage
	 * @param id the channel id
	 * @param name channel name
	 * @param isSnapshot whether this channels type is snapshot or focused
	 * @param isDefault whether this channel is the default one for the given type
	 * @param comments user comments
	 * @throws ArgumentNotValid if the name is incorrect.
	 */
	public HarvestChannel(long id, String name, boolean isSnapshot, boolean isDefault, String comments) {
		ArgumentNotValid.checkNotNullOrEmpty(name, "name");
		if (!isAcceptableName(name)) {
			throw new ArgumentNotValid("'" + name + "' does not match pattern '"
					+ ACCEPTABLE_NAME_PATTERN + "'");
		}
		this.id = id;
		this.name = name;
		this.isSnapshot = isSnapshot;
		this.isDefault = isDefault;
		this.comments = comments;
	}

	/**
	 * @return the unique identifier in the persistent storage
	 */
	public long getId() {
		return id;
	}

	/**
	 * @return the harvest channel name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the harvest channel name
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return true if this channel is intended for snaphsot harvests, false if it is intended for
	 * focused ones.
	 */
	public boolean isSnapshot() {
		return isSnapshot;
	}

	/**
	 * Set the harvest type to snapshot or focused.
	 * @param isSnapshot true if snapshot, false if focused
	 */
	public void setSnapshot(boolean isSnapshot) {
		this.isSnapshot = isSnapshot;
	}

	/**
	 * @return true if the channel is the default one for the harvest type (snapshot or focused),
	 * false otherwise.
	 */
	public boolean isDefault() {
		return isDefault;
	}

	/**
	 * Set whether if the channel is the default one for the harvest type (snapshot or focused).
	 * @param isDefault true if default, false otherwise
	 */
	public void setDefault(boolean isDefault) {
		this.isDefault = isDefault;
	}

	/**
	 * @return the associated comments.
	 */
	public String getComments() {
		return comments;
	}

	/**
	 * Sets the associated comments 
	 * @param comments the comments to set
	 */
	public void setComments(String comments) {
		this.comments = comments;
	}

	/**
	 * Renders a localized description for the {@link #SNAPSHOT} singleton.
	 * @param context
	 * @return a localized description.
	 */
	public static String getSnapshotDescription(PageContext context) {
		return I18n.getString(
				dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE,
				context.getResponse().getLocale(),
				"harvest.channel.snapshot.desc");
	}

	/**
	 * Returns true if the given input is an acceptable channel name.
	 * @param input the candidate name.
	 * @return true if the name complies to the defined {@link #ACCEPTABLE_NAME_PATTERN}, false
	 * otherwise
	 */
	public static boolean isAcceptableName(String input) {
		return input.matches(ACCEPTABLE_NAME_PATTERN);
	}

	@Override
	public String toString() {
		return "HarvestChannel [id=" + id + ", name=" + name + ", comments=" + comments
				+ ", isSnapShot=" + isSnapshot + ", isDefault=" + isDefault + "]";
	}

}
