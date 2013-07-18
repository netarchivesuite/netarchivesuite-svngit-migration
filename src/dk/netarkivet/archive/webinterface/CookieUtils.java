/**
 * 
 */
package dk.netarkivet.archive.webinterface;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
public class CookieUtils {

	/**
	 * Some cookie lifespan to play with.
	 */
	public static enum Lifespan {

		MINUTE(60),
		HOUR(60 * 60),
		DAY(24 * 60 * 60),
		WEEK(7 * 24 * 60 * 60);

		private final int seconds;

		private Lifespan(int seconds) {
			this.seconds = seconds;
		}

		public int getSeconds() {
			return seconds;
		}	

	}

	/**
	 * Returns the value of a request parameter, or if not found 
	 * tries to find a cookie with the same name.
	 * @param request the HTTP request
	 * @param name the parameter name
	 * @return the value (never null, may be empty)
	 */
	public static final String getParameterValue(HttpServletRequest request, String name) {
		String value = request.getParameter(name);
		if (value == null || value.isEmpty()) {
			for (Cookie c : request.getCookies()) {
				if (name.equals(c.getName())) {
					value = c.getValue();
				}
			}
		}
		return (value != null ? value : "");
	}

	/**
	 * Set a cookie on the client.
	 * @param response the HTTP response wrapper
	 * @param name the cookie name
	 * @param value the cookie value
	 * @param lifeSpan the cookie TTL as an {@link Lifespan} enum value
	 */
	public static final void setCookie(
			HttpServletResponse response,
			String name, 
			String value, 
			Lifespan lifeSpan) {
		Cookie c = new Cookie(name, value);
		c.setMaxAge(lifeSpan.getSeconds());
		response.addCookie(c);
	}

	/**
	 * Set a cookie on the client, with a default lifespan of @see Lifespan#HOUR
	 * @param response the HTTP response wrapper
	 * @param name the cookie name
	 * @param value the cookie value
	 */
	public static final void setCookie(
			HttpServletResponse response,
			String name, 
			String value) {
		setCookie(response, name, value, Lifespan.HOUR);
	}

}
