/*$Id$
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
package dk.netarkivet.common.exceptions;

/**
 * An input/output operation failed.
 */
public class IOFailure extends NetarkivetException  {
    /**
     * Constructs new IOFailure with the specified detail message.
     * @param message The detail message
     */
    public IOFailure(String message) {
        super(message);
    }

    /**
     * Constructs new IOFailure with the specified detail message and cause.
     * @param message The detail message
     * @param cause The cause
     */
    public IOFailure(String message, Throwable cause) {
        super(message, cause);
    }
}
