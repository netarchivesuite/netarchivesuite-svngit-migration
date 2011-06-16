/* $Id$
 * $Date$
 * $Revision$
 * $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.common.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Properties;

/**
 * This executable takes three parameters :
 * <ol>
 * <li>base properties file, that defines the key ordering</li>
 * <li>properties file to reformat, sorting keys in the order defined by the first file</li>
 * <li>character encoding for reformat and output file</li>
 * </ol>
 *
 * The second file is overwritten
 *
 */
public class ReformatTranslationFile {

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        if (args.length != 3) {
            System.out.println("Usage: java "
                    + ReformatTranslationFile.class.getName()
                    + "\n\t<properties file defining key order>"
                    + "\n\t<properties file to fetch values from>"
                    + "\n\t<second and output file encoding>");
            System.exit(1);
        }

        File order = new File(args[0]);
        File reformat = new File(args[1]);
        String encoding = args[2];

        Properties defaultProps = new Properties();
        defaultProps.load(new FileInputStream(order));

        Properties sourceProps = new Properties();
        sourceProps.load(new FileInputStream(reformat));

        if (reformat.delete()) {
            reformat.createNewFile();
        }
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(reformat), encoding));

        BufferedReader in = new BufferedReader(new FileReader(order));
        String line = null;
        while ((line = in.readLine()) != null) {
            if (line.indexOf("=") == -1) {
                out.write(line);
                out.newLine();
            } else {
                String[] parts = line.split("=");
                String key = parts[0].trim();
                out.write(key + "=");
                for (char c : parts[1].toCharArray()) {
                    if (Character.isWhitespace(c)) {
                        out.write(c);
                    } else {
                        break;
                    }
                }
                String propVal = sourceProps.getProperty(key);
                if (propVal == null) {
                    System.out.println(
                            "No value for key '" + key + "' in right file");
                    out.write(defaultProps.getProperty(key));
                } else {
                    out.write(propVal);
                }
                out.newLine();

            }
        }

        in.close();
        out.close();

        System.out.println("Successfully reformatted "
                + reformat.getAbsolutePath());
        System.exit(0);
    }

}
