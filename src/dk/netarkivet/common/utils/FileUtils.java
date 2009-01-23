/* $Id$
 * $Revision$
 * $Author$
 * $Date$
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
package dk.netarkivet.common.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.Constants;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.exceptions.UnknownID;

/**
 * Misc. handy file utilities.
 */
public class FileUtils {
    /** Extension used for CDX files, including separator . */
    public static final String CDX_EXTENSION = ".cdx";
    /** Extension used for ARC files, including separator . */
    public static final String ARC_EXTENSION = ".arc";
    /** Extension used for ARC files, including separator . */
    public static final String ARC_GZIPPED_EXTENSION = ".arc.gz";

    /** Pattern matching ARC files, including separator.
     * Note: (?i) means case insensitive, (\\.gz)? means .gz is optionally
     * matched, and $ means matches end-of-line. Thus this pattern will match
     * file.arc.gz, file.ARC, file.aRc.GZ, but not
     * file.ARC.open */
    public static final String ARC_PATTERN = "(?i)\\.arc(\\.gz)?$";
    /** Pattern matching open ARC files, including separator .
     * Note: (?i) means case insensitive, (\\.gz)? means .gz is optionally
     * matched, and $ means matches end-of-line. Thus this pattern will match
     * file.arc.gz.open, file.ARC.open, file.arc.GZ.OpEn, but not
     * file.ARC.open.txt */
    public static final String OPEN_ARC_PATTERN = "(?i)\\.arc(\\.gz)?\\.open$";

    public static final Log log =
            LogFactory.getLog(FileUtils.class.getName());

    /**
     * A FilenameFilter accepting a file if and only if
     * its name (transformed to lower case) ends on ".cdx".
     */
    public static final FilenameFilter CDX_FILE_FILTER
            = new FilenameFilter() {
                public boolean accept(File directory, String filename) {
                    return filename.toLowerCase().endsWith(CDX_EXTENSION);
                }
            };


    /** A filter that matches files left open by a crashed Heritrix process.
     * Don't work on these files while Heritrix is still working on them.
     */
    public static final FilenameFilter OPEN_ARCS_FILTER =
            new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.matches(".*" + OPEN_ARC_PATTERN);
                }
            };

    /** A filter that matches arc files, that is any file that ends on .arc or
     * .arc.gz in any case. */
    public static final FilenameFilter ARCS_FILTER =
            new FilenameFilter() {
                public boolean accept(File directory, String filename) {
                    return filename.toLowerCase().matches(".*" + ARC_PATTERN);
                }
            };

    /** How many times we will retry making a unique directory name. */
    private static final int MAX_RETRIES = 10;
    /** How many times we will retry making a directory. */
    private static final int CREATE_DIR_RETRIES = 3;
    /** Maximum number of IDs we will put in a filename.  Above this
     * number, a checksum of the ids is generated instead.  This is done
     * to protect us from getting filenames too long for the filesystem.
     */
    public static final int MAX_IDS_IN_FILENAME = 4;

    /**
     * Remove a file and any subfiles in case of directories.
     *
     * @param f
     *            A file to completely and utterly remove.
     * @return true if the file did exist, false otherwise.
     * @throws SecurityException
     *             If a security manager exists and its <code>{@link
     *                           java.lang.SecurityManager#checkDelete}</code>
     *             method denies delete access to the file
     */
    public static boolean removeRecursively(File f) {
        ArgumentNotValid.checkNotNull(f, "File f");
        if (!f.exists()) {
            return false;
        }

        // If the file is a directory, delete all files in this directory,
        // and its subdirectories
        if (f.isDirectory()) {
            File[] subfiles = f.listFiles();

            if (subfiles != null) { // Can be null in case of error
                for (File subfile : subfiles) {
                    removeRecursively(subfile);
                }
            }
        }
        if (!f.delete()) {
            boolean isDir = f.isDirectory();
            if (!isDir) {
                log.debug("Try once more deleting file '" 
                        + f.getAbsolutePath());
                final boolean success = remove(f);
                if (!success) {
                        throw new IOFailure("Unable to remove file: '" 
                                        + f.getAbsolutePath() + "'");
                }
            } else {
                String errMsg = "Problem with deletion of directory: '" 
                    + f.getAbsolutePath() + "'.";
                log.debug(errMsg);
                throw new IOFailure(errMsg);
            }
        }

        return true;
    }
    
    /**
     * Remove a file.
     * @param f
     *            A file to completely and utterly remove.
     * @return true if the file did exist, false otherwise.
     * @throws ArgumentNotValid if f is null.
     * @throws SecurityException
     *             If a security manager exists and its <code>{@link
     *                           java.lang.SecurityManager#checkDelete}</code>
     *             method denies delete access to the file
     */
    public static boolean remove(File f) {
        ArgumentNotValid.checkNotNull(f, "f");
        if (!f.exists()) {
            return false;
        }
        if (f.isDirectory()) {
            return false; //Do not attempt to delete a directory
        }
        if (!f.delete()) {
            // Hack to remove file on windows! Works only sometimes!
            File delFile = new File(f.getAbsolutePath());
            delFile.delete();
            if (delFile.exists()) {
                throw new IOFailure("Unable to delete file '"
                        + f.getAbsolutePath());
            }
        }

        return true;
    }
    
    /**
     * Returns a valid filename for most filesystems. Exchanges the following
     * characters: <p/> " " -> "_" ":" -> "_" "+" -> "_"
     *
     * @param filename
     *            the filename to format correctly
     * @return a new formatted filename
     */
    public static String formatFilename(String filename) {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        String formattedFilename = filename;

        // remove spaces
        formattedFilename = formattedFilename.replace(' ', '_');

        // remove colons
        formattedFilename = formattedFilename.replace(':', '_');

        // remove add sign
        formattedFilename = formattedFilename.replace('+', '_');

        return formattedFilename;
    }

    /**
     * Retrieves all files whose names ends with 'type' from directory 'dir' and
     * all its subdirectories.
     *
     * @param dir
     *            Path of base directory
     * @param files
     *            Initially, an empty list (e.g. an ArrayList)
     * @param type
     *            The extension/ending of the files to retrieve (e.g. ".xml",
     *            ".ARC")
     * @return A list of files from directory 'dir' and all its subdirectories
     */
    public static List<File> getFilesRecursively(
            String dir, List<File> files, String type) {
        ArgumentNotValid.checkNotNullOrEmpty(dir, "String dir");
        File theDirectory = new File(dir);
        ArgumentNotValid.checkTrue(theDirectory.isDirectory(),
                "File '" + theDirectory.getAbsolutePath()
                + "' does not represent a directory");
        ArgumentNotValid.checkNotNull(files, "files");
        ArgumentNotValid.checkNotNull(type, "type");
         
        File[] top = new File(dir).listFiles();
        for (File aTop : top) {
            if (aTop.isDirectory()) {
                getFilesRecursively(aTop.getAbsolutePath(), files, type);
            } else if (aTop.isFile() && aTop.getName().endsWith(type)) {
                files.add(aTop);
            }
        }

        return files;
    }

    /**
     * Load file content into text string.
     *
     * @param file The file to load
     * @return file content loaded into text string
     * @throws java.io.FileNotFoundException if the file cannot be found.
     * @throws java.io.IOException on IO trouble reading the file.
     */
    public static String readFile(File file) throws IOException {
        ArgumentNotValid.checkNotNull(file, "File file");
        StringBuffer sb = new StringBuffer();

        BufferedReader br = new BufferedReader(new FileReader(file));

        try {
            int i;

            while ((i = br.read()) != -1) {
                sb.append((char) i);
            }
        } finally {
            br.close();
        }

        return sb.toString();
    }

    /**
     * Copy file from one location to another. Will silently overwrite an
     * already existing file.
     *
     * @param from
     *            original to copy
     * @param to
     *            destination of copy
     * @throws IOFailure if an io error occurs while copying file.
     */
    public static void copyFile(File from, File to) {
        ArgumentNotValid.checkNotNull(from, "File from");
        ArgumentNotValid.checkNotNull(to, "File to");
        try {
            FileInputStream inStream = null;
            FileOutputStream outStream = null;
            FileChannel in = null;
            FileChannel out = null;
            try {
                inStream = new FileInputStream(from);
                outStream = new FileOutputStream(to);
                in = inStream.getChannel();
                out = outStream.getChannel();
                long bytesTransferred = 0;
                do {
                    //Note: in.size() is called every loop, because if it should
                    //change size, we might end up in an infinite loop trying to
                    //copy more bytes than are actually available.
                    bytesTransferred += in.transferTo(bytesTransferred,
                            Math.min(Constants.IO_CHUNK_SIZE,
                                     in.size() - bytesTransferred),
                            out);
                } while (bytesTransferred < in.size());
            } finally {
                if (inStream != null) {
                    inStream.close();
                }
                if (outStream != null) {
                    outStream.close();
                }
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            }
        } catch (IOException e) {
            throw new IOFailure("Error copying file '" + from.getAbsolutePath()
                    + "' to '" + to.getAbsolutePath() + "'", e);
        }
    }

    /**
     * Copy an entire directory from one location to another. Note that this
     * will silently overwrite old files, just like copyFile().
     *
     * @param from
     *            Original directory (or file, for that matter) to copy.
     * @param to
     *            Destination directory, i.e. the 'new name' of the copy of the
     *            from directory.
     * @throws IOFailure On IO trouble copying files.
     */
    public static void copyDirectory(File from, File to) throws IOFailure {
        ArgumentNotValid.checkNotNull(from, "File from");
        ArgumentNotValid.checkNotNull(to, "File to");
        if (from.isFile()) {
            try {
                copyFile(from, to);
            } catch (Exception e) {
                throw new IOFailure("Error copying from "
                        + from.getAbsolutePath() + " to "
                        + to.getAbsolutePath(), e);
            }
        } else {
            if (!from.exists()) {
                throw new IOFailure("Can't find directory " + from);
            }

            if (!from.isDirectory()) {
                throw new IOFailure("File is not a directory: " + from);
            }

            to.mkdir();

            if (!to.exists()) {
                throw new IOFailure("Failed to create directory " + to);
            }

            File[] subfiles = from.listFiles();

            for (File subfile : subfiles) {
                copyDirectory(subfile, new File(to, subfile.getName()));
            }
        }
    }

    /**
     * Read an entire file, byte by byte, into a byte array, ignoring any locale
     * issues.
     *
     * @param file A file to be read.
     * @return A byte array with the contents of the file.
     * @throws IOFailure on IO trouble reading the file
     */
    public static byte[] readBinaryFile(File file) throws IOFailure {
        ArgumentNotValid.checkNotNull(file, "File file");
        
        if (file.length() > Integer.MAX_VALUE) {
            throw new IndexOutOfBoundsException(
                    "File too long to fit in array: " + file.length()
                            + " bytes in " + file);
        }

        byte[] result = new byte[(int) file.length()];
        FileInputStream in = null;
        try {
            try {
                in = new FileInputStream(file);
                int bytesRead;
                for (int i = 0;
                     i < result.length 
                         && (bytesRead = in.read(result, i, result.length - i))
                                  != -1;
                     i += bytesRead) {
                }
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        } catch (IOException e) {
            throw new IOFailure("Error reading " + file.getAbsolutePath(), e);
        }

        return result;
    }

    /**
     * Write an entire byte array to a file, ignoring any locale issues.
     *
     * @param file
     *            The file to write the data to
     * @param b
     *            The byte array to write to the file
     */
    public static void writeBinaryFile(File file, byte[] b) {
        ArgumentNotValid.checkNotNull(file, "File file");
        ArgumentNotValid.checkNotNull(b, "byte[] b");
        FileOutputStream out = null;
        try {
            try {
                out = new FileOutputStream(file);
                out.write(b);
            } finally {
                if (out != null) {
                    out.close();
                }
            }
        } catch (Exception e) {
            throw new IOFailure("writeBinaryFile exception: " + e, e);
        }
    }

    /**
     * Return a filter that only accepts XML files (ending with .xml).
     *
     * @return A new filter for XML files.
     */
    public static FilenameFilter getXmlFilesFilter() {
        return new FilenameFilter() {

            /**
             * Tests if a specified file should be included in a file list.
             *
             * @param dir
             *            the directory in which the file was found.
             * @param name
             *            the name of the file.
             * @return <code>true</code> if and only if the name should be
             *         included in the file list; <code>false</code>
             *         otherwise.
             * @see FilenameFilter#accept(java.io.File, java.lang.String)
             */
            public boolean accept(File dir, String name) {
                return name.endsWith(Constants.XML_EXTENSION);
            }
        };
    }

    /**
     * Read a all lines from a file into a list of strings.
     * @param file The file to read from.
     * @return The list of lines.
     * @throws IOFailure on trouble reading the file.
     */
    public static List<String> readListFromFile(File file) {
        ArgumentNotValid.checkNotNull(file, "File file");
        List<String> lines = new ArrayList<String>();
        BufferedReader in = null;
        try {
            try {
                in = new BufferedReader(new FileReader(file));
                String line;
                while ((line = in.readLine()) != null) {
                    lines.add(line);
                }
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        } catch (IOException e) {
            String msg = "Could not read data from "
                         + file.getAbsolutePath();
            log.warn(msg, e);
            throw new IOFailure(msg, e);
        }
        return lines;
    }

    /** Writes a collection of strings to a file, each string on one line.
     *
     * @param file A file to write to.  The contents of this file will be
     * overwritten.
     * @param collection The collection to write.  The order it will be
     * written in is unspecified.
     * @throws IOFailure if any error occurs writing to the file.
     * @throws ArgumentNotValid if file or collection is null.
     */
    public static void writeCollectionToFile(
            File file, Collection<String> collection) {
        ArgumentNotValid.checkNotNull(file, "file");
        ArgumentNotValid.checkNotNull(collection, "collection");
        try {
            PrintWriter writer = null;
            try {
                writer = new PrintWriter(new FileWriter(file));
                for (String fileName : collection) {
                    writer.println(fileName);
                }
                writer.flush();
            } finally {
                if (writer != null) {
                    writer.close();
                }
            }
        } catch (IOException e) {
            String msg = "Error writing collection to file '"
                    + file.getAbsolutePath() + "'";
            log.warn(msg, e);
            throw new IOFailure(msg, e);
        }
    }

    /** Sort a file into another.  The current implementation slurps all lines
     * into memory.  This will not scale forever.
     *
     * @param unsortedFile A file to sort
     * @param sortedOutput The file to sort into
     */
    public static void makeSortedFile(File unsortedFile, File sortedOutput) {
        ArgumentNotValid.checkNotNull(unsortedFile, "File unsortedFile");
        ArgumentNotValid.checkNotNull(sortedOutput, "File sortedOutput");
        List<String> lines;
        lines = readListFromFile(unsortedFile);
        Collections.sort(lines);
        writeCollectionToFile(sortedOutput, lines);
    }

    /** Remove a line from a given file.
     *
     * @param line The full line to remove
     * @param file The file to remove the line from.  This file will be
     * rewritten in full, and the entire contents will be kept in memory
     * @throws UnknownID If the file does not exist
     */
    public static void removeLineFromFile(String line, File file) {
        if (!file.exists()) {
            throw new UnknownID("The file '" + file + "' does not exist.");
        }

        List<String> lines = readListFromFile(file);
        lines.remove(line);
        writeCollectionToFile(file, lines);
    }

    /**
     * Check if the directory exists and is writable and create it if needed.
     * The complete path down to the directory is created. If the directory
     * creation fails a PermissionDenied exception is thrown.
     *
     * @param dir The directory to create
     * @throws ArgumentNotValid If dir is null or its name is the empty string
     * @throws PermissionDenied If directory cannot be created for any reason,
     * or is not writable.
     * @return true if dir created.
     */
    public static boolean createDir(File dir) throws PermissionDenied {
        ArgumentNotValid.checkNotNull(dir, "File dir");
        ArgumentNotValid.checkNotNullOrEmpty(dir.getName(), "File dir");
        boolean didCreate = false;
        if (!dir.exists()) {
            didCreate = true;
            int i = 0;
            //retrying creation due to sun bug (race condition)
            //See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4742723
            while ((i++ < CREATE_DIR_RETRIES)
                           && !(dir.isDirectory() && dir.canWrite())) {
                dir.mkdirs();
            }
            if (!(dir.isDirectory() && dir.canWrite())) {
                String msg = "Could not create directory: '"
                        + dir + "'";
                log.warn(msg);
                throw new PermissionDenied(msg);
            }
        } else {
            if (!dir.isDirectory()) {
                String msg = "Cannot make directory '" + dir + "' - a file "
                        + "is in the way";
                log.warn(msg);
                throw new PermissionDenied(msg);
            }
        }
        if (!dir.canWrite()) {
            String msg = "Cannot write to required directory '" + dir + "'";
            log.warn(msg);
            throw new PermissionDenied(msg);
        }
        return didCreate;
    }

    /**
     * Returns the number of bytes free on the file system that the given file
     * resides on. Will return 0 on non-existing files.
     *
     * @param f a given file
     * @return the number of bytes free on the file system where file f resides.
     * 0 if the file cannot be found.
     */
    public static long getBytesFree(File f) {
        ArgumentNotValid.checkNotNull(f, "f");
        if (!f.exists()) {
            return 0;
        }
        return f.getUsableSpace();
    }

    /**
     * @param theFile
     *            A file to make relative
     * @param crawlDir
     *            A directory
     * @return the filepath of the theFile relative to crawldir. null, if
     *         theFile is not relative to crawldir. null, if crawldir is not a
     *         directory.
     */

    public static String relativeTo(File theFile, File crawlDir) {
        if (!crawlDir.isDirectory()) {
            // System.out.println(crawlDir + " is no dir");
            return null;
        }

        List<String> filePathList = new ArrayList<String>();
        List<String> crawlDirPath = new ArrayList<String>();
        File tempFile = theFile.getAbsoluteFile();

        filePathList.add(tempFile.getName());
        while ((tempFile = tempFile.getParentFile()) != null) {
            filePathList.add(tempFile.getName());
        }

        tempFile = crawlDir.getAbsoluteFile();
        crawlDirPath.add(tempFile.getName());
        while ((tempFile = tempFile.getParentFile()) != null) {
            crawlDirPath.add(tempFile.getName());
        }

        // check, at the path prefix is the same
        List<String> sublist = filePathList.subList(crawlDirPath.size() - 2,
                filePathList.size());
        if (!crawlDirPath.equals(sublist)) {
            // System.out.println("is not equal");
            // System.out.println("crawldirPath: " + crawlDirPath);
            // System.out.println("sublist: " + sublist);
            return null;
        }

        List<String> relativeList
                = filePathList.subList(0, crawlDirPath.size() - 2);

        StringBuffer sb = new StringBuffer();
        Collections.reverse(relativeList);
        for (String aRelativeList : relativeList) {
            sb.append(aRelativeList);
            sb.append(File.separatorChar);
        }
        sb.deleteCharAt(sb.length() - 1); // remove last separatorChar
        return sb.toString();

    }

    /**
     * Count the number of lines in a file.
     * @param file the file to read
     * @return the number of lines in the file
     */
    public static long countLines(File file) {
        ArgumentNotValid.checkNotNull(file, "file");
        BufferedReader in = null;
        long count = 0;
        try {
            try {
                in = new BufferedReader(new FileReader(file));
                while (in.readLine() != null) {
                    count++;
                }
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        } catch (IOException e) {
            String msg = "Could not check number of lines in '"
                    + file.getAbsolutePath() + "'";
            log.warn(msg, e);
            throw new IOFailure(msg, e);
        }
        return count;
    }

    /** Create an InputStream that reads from a file but removes the file
     *  when all data has been read.
     *
     * @param file A file to read.  This file will be deleted when the
     * inputstream is closed, finalized, reaches end-of-file, or when the
     * VM closes.
     * @return An InputStream containing the file's contents.
     */
    public static InputStream getEphemeralInputStream(final File file) {
        ArgumentNotValid.checkNotNull(file, "file");
        // First make sure we remove the file if the VM dies
        file.deleteOnExit();
        try {
            // Then create an input stream that deletes the file upon exit.
            // Note that FileInputStream.finalize calls close().
            return new FileInputStream(file) {
                public void close() throws IOException {
                    super.close();
                    file.delete();
                }
            };
        } catch (IOException e) {
            String msg = "Error creating ephemeral input stream for " + file;
            log.warn(msg, e);
            throw new IOFailure(msg, e);
        }
    }

    /**
     * Makes a valid file from filename passed in String. Ensures that the File
     * object returned is not null, and that isFile() returns true.
     *
     * @param filename The file to create the File object from
     * @return A valid, non-null File object.
     * @throws IOFailure if file cannot be created.
     */
    public static File makeValidFileFromExisting(String filename)
            throws IOFailure {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        
        File res = new File(filename);
        if (!res.isFile()) {
            throw new IOFailure("Error: File object created from filename '"
                                + filename
                                + "' is not a proper file, isFile() failed.");
        }
        return res;
    }

    /** Write the entire contents of a file to a stream.
     *
     * @param f A file to write to the stream.
     * @param out The stream to write to.
     */
    public static void writeFileToStream(File f, OutputStream out) {
        ArgumentNotValid.checkNotNull(f, "File f");
        ArgumentNotValid.checkNotNull(out, "OutputStream out");

        byte[] buffer = new byte[Constants.IO_BUFFER_SIZE];
        try {
            FileInputStream in = new FileInputStream(f);
            try {
                int bytesRead;
                while ((bytesRead = in.read(buffer)) > 0) {
                    out.write(buffer, 0, bytesRead);
                }
            } finally {
                in.close();
            }
        } catch (IOException e) {
            throw new IOFailure("Error writing file '" + f + "' to stream", e);
        }
    }

    /** Write the contents of a stream into a file.
     *
     * @param in A stream to read from.  This stream is not closed by this
     * method.
     * @param f The file to write the stream contents into.
     */
    public static void writeStreamToFile(InputStream in, File f) {
        ArgumentNotValid.checkNotNull(f, "File f");
        ArgumentNotValid.checkNotNull(in, "InputStream in");

        byte[] buffer = new byte[Constants.IO_BUFFER_SIZE];
        try {
            FileOutputStream out = new FileOutputStream(f);
            try {
                int bytesRead;
                while ((bytesRead = in.read(buffer)) > 0) {
                    out.write(buffer, 0, bytesRead);
                }
            } finally {
                out.close();
            }
        } catch (IOException e) {
            throw new IOFailure("Error writing stream to file '" + f + "'", e);
        }
    }

    /** Get the location of the standard temporary directory.
     *  The
     * existence of this directory should be ensure at the start of every
     * application.
     *
     * @return The directory that should be used for temporary files.
     */
    public static File getTempDir() {
        return new File(Settings.get(CommonSettings.DIR_COMMONTEMPDIR));
    }

    /**
     * Attempt to move a file using rename, and if that fails, move the file
     * by copy-and-delete.
     * @param fromFile The source
     * @param toFile The target
     */
    public static void moveFile(File fromFile, File toFile) {
        ArgumentNotValid.checkNotNull(fromFile, "File fromFile");
        ArgumentNotValid.checkNotNull(toFile, "File toFile");
        
        if (!fromFile.renameTo(toFile)) {
            copyFile(fromFile, toFile);
            remove(fromFile);
        }
    }

    /** Given a set, generate a reasonable file name from the set.
     *
     * @param IDs A set of IDs.
     * @param suffix A suffix.
     * @return A reasonable file name.
     */
    public static <T extends Comparable<T>> String generateFileNameFromSet(
            Set<T> IDs, String suffix) {
        ArgumentNotValid.checkNotNull(IDs, "Set<T> IDs");

        if (IDs.isEmpty()) {
            return "empty" + suffix;
        }

        List<T> sorted = new ArrayList<T>(IDs);
        Collections.sort(sorted);

        String allIDsString = StringUtils.conjoin("-", sorted);
        String fileName;
        if (sorted.size() > MAX_IDS_IN_FILENAME) {
            String firstNIDs = StringUtils.conjoin("-", sorted.subList(
                    0, MAX_IDS_IN_FILENAME));
            fileName = firstNIDs + "-"
                              + MD5.generateMD5(allIDsString.getBytes())
                              + suffix;
        } else {
            fileName = allIDsString + suffix;
        }
        return fileName;
    }

    /** Sort a crawl.log file according to URL.  This method depends on
     * the Unix sort() command.
     *
     * @param file The file containing the unsorted data.
     * @param toFile The file that the sorted data can be put into.
     * @throws IOFailure if there were errors running the sort process.
     */
    public static void sortCrawlLog(File file, File toFile) {
        ArgumentNotValid.checkNotNull(file, "File file");
        ArgumentNotValid.checkNotNull(toFile, "File toFile");
        
        int error = ProcessUtils.runProcess(new String[]{"LANG=C"},
                // -k 4b means fourth field (from 1) ignoring leading blanks
                // -o means output to (file)
                "sort", "-k", "4b",
                file.getAbsolutePath(),
                "-o", toFile.getAbsolutePath());
        if (error != 0) {
            throw new IOFailure("Error code " + error + " sorting crawl log '"
                    + file + "'");
        }
    }

    /** Sort a CDX file according to our standard for CDX file sorting.  This
     * method depends on the Unix sort() command.
     *
     * @param file The raw unsorted CDX file.
     * @param toFile The file that the result will be put into.
     */
    public static void sortCDX(File file, File toFile) {
        ArgumentNotValid.checkNotNull(file, "File file");
        ArgumentNotValid.checkNotNull(toFile, "File toFile");
        
        int error = ProcessUtils.runProcess(new String[] {"LANG=C"},
                "sort", file.getAbsolutePath(),
                "-o", toFile.getAbsolutePath());
        if (error != 0) {
            throw new IOFailure("Error code " + error + " sorting cdx file '"
                    + file + "'");
        }
    }

    /**
     * A class for parsing an ARC filename as generated by our runs of Heritrix
     * and retrieving components like harvestID and jobID.
     * See HeritrixLauncher.getCrawlID for where the format gets defined.
     */
    public static class FilenameParser{
        /** Our file names contain jobID, harvestID, timestamp and serial no.
         *
         */
        private static final Pattern FILE_NAME_PATTERN =
            Pattern.compile("(\\d+)\\-(\\d+)\\-(\\d+)\\-(\\d+)\\-.*");

        /** pattern group containing the Job ID. */
        private static final int JOB_ID = 1;
        /** pattern group containing the harvest ID. */
        private static final int HARVEST_ID = 2;
        /** pattern group containing the timestamp. */
        private static final int TIME_STAMP = 3;
        /** pattern group containing the serial number. */
        private static final int SERIAL_NO = 4;

        /** Field containing the parsed harvest ID. */
        private final String harvestID;
        /** Field containing the parsed job Id. */
        private final String jobID;
        /** Field containing the timestamp. */
        private final String timeStamp;
        /** Field containing the serial number. */
        private final String serialNo;

        /** Field containing the original filename. */
        private final String filename;
        /**
         * Parser the name of the given file.
         * @param file An ARC/CDX file named following Netarkivets convention.
         * @throws UnknownID if the file was NOT named following
         * Netarkivets convention.
         */
        public FilenameParser(File file) throws UnknownID {
            ArgumentNotValid.checkNotNull(file, "File file");
            try {
                filename = file.getName();
                Matcher m = FILE_NAME_PATTERN.matcher(file.getName());
                if (m.matches()){
                    harvestID = m.group(HARVEST_ID);
                    jobID = m.group(JOB_ID);
                    timeStamp = m.group(TIME_STAMP);
                    serialNo = m.group(SERIAL_NO);
                } else {
                    throw new UnknownID("Could not parse " + file.getName());
                }
            } catch (RuntimeException e) {
                throw new UnknownID("Could not parse " + file.getName(), e);
            }
        }
        
        /** 
         * Get the harvestID.
         * @return the harvestID.
         */
        public String getHarvestID() {
            return harvestID;
        }
        
        /** 
         * Get the job ID.
         * @return the Job ID.
         */
        public String getJobID() {
            return jobID;
        }
        
        /** 
         * Get the timestamp.
         * @return the timestamp.
         */
        public String getTimeStamp() {
            return timeStamp;
        }
        
        /** 
         * Get the serial number.
         * @return the serial number.
         */
        public String getSerialNo() {
            return serialNo;
        }
        
        /** 
         * Get the filename.
         * @return the filename.
         */
        public String getFilename() {
            return filename;
        }
    }

    /** Creates a new temporary directory with a unique name.
     * This directory will be deleted automatically at the end of the
     * VM (though behaviour if there are files in it is undefined).
     * This method will try a limited number of times to create a directory,
     * using a randomly generated suffix, before giving up.
     *
     * @param inDir The directory where the temporary directory
     * should be created.
     * @param prefix The prefix of the directory name, for identification
     * purposes.
     * @return A newly created directory that no other calls to createUniqueDir
     * returns.
     * @throws ArgumentNotValid if inDir is not an existing
     * directory that can be written to.
     * @throws IOFailure if a free name couldn't be found within a reasonable
     * number of tries.
     */
    public static File createUniqueTempDir(File inDir, String prefix) {
        ArgumentNotValid.checkNotNull(inDir, "File inDir");
        ArgumentNotValid.checkNotNullOrEmpty(prefix, "String prefix");
        ArgumentNotValid.checkTrue(inDir.isDirectory(),
                inDir + " must be a directory");
        ArgumentNotValid.checkTrue(inDir.canWrite(),
                inDir + " must be writeable");
        for (int tries = 0; tries < MAX_RETRIES; tries++) {
            File newDir;
            try {
                newDir = File.createTempFile(prefix, null, inDir);
            } catch (IOException e) {
                throw new IOFailure("Couldn't create temporary file in '"
                        + inDir + "' with prefix '" + prefix + "'", e);
            }
            newDir.delete();
            if (newDir.mkdir()) {
                newDir.deleteOnExit();
                return newDir;
            }
        }
        throw new IOFailure("Too many similar files around, cannot create "
                + "unique dir with prefix " + prefix + " in " + inDir);
    }

    /**
     * Read the last line in a file. Note this method is not UTF-8 safe.
     *
     * @param file input file to read last line from.
     * @return The last line in the file (ending newline is irrelevant),
     * returns an empty string if file is empty.
     * @throws ArgumentNotValid on null argument, or file is not a readable
     * file.
     * @throws IOFailure on IO trouble reading file.
     */
    public static String readLastLine(File file) {
        ArgumentNotValid.checkNotNull(file, "File file");
        if (!file.isFile() || !file.canRead()) {
            throw new ArgumentNotValid("File '" + file.getAbsolutePath()
                                       + "' is not a readable file.");
        }
        if (file.length() == 0) {
            return "";
        }
        RandomAccessFile rafile = null;
        try {
            rafile = new RandomAccessFile(file, "r");
            //seek to byte one before end of file (remember we know the file is
            // not empty) - this ensures that an ending newline is not read
            rafile.seek(rafile.length() - 2);
            //now search to the last linebreak, or beginning of file
            while (rafile.getFilePointer() != 0 && rafile.read()!='\n') {
                //search back two, because we just searched forward one to find
                //newline
                rafile.seek(rafile.getFilePointer() - 2);
            }
            return rafile.readLine();
        } catch (IOException e) {
            throw new IOFailure("Unable to access file '"
                                + file.getAbsolutePath() + "'", e);
        } finally {
            try {
                if (rafile != null) {
                    rafile.close();
                }
            } catch (IOException e) {
                log.debug("Unable to close file '"
                                       + file.getAbsolutePath()
                                       + "' after reading", e);
            }
        }
    }

    /** Append the given lines to a file.  Each lines is terminated by a
     * newline.
     *
     * @param file A file to append to.
     * @param lines The lines to write.
     * @throws IOFailure if anything goes wrong during the writing process
     */
    public static void appendToFile(File file, String... lines) {
        ArgumentNotValid.checkNotNull(file, "File file");
        ArgumentNotValid.checkNotNull(lines, "String... lines");
        
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter(file, true));
            for (String line : lines) {
                writer.println(line);
            }
        } catch (IOException e) {
            log.warn("Error appending " + lines.length + " lines to file '"
                    + file + "'",
                     e);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }

    }
    
    /**
     * Loads an file from the class path (for retrieving a file from '.jar').
     * 
     * @param filePath The path of the class.
     * @return The file as an input stream.
     * @throws IOException 
     */
    public static File getResourceFileFromClassPath(String filePath) 
            throws IOException {
        ArgumentNotValid.checkNotNullOrEmpty(
                filePath,
                "String defaultClasspathSettingsPath");
        InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(filePath);
        if (stream != null) {
            File tmpFile = File.createTempFile("tmp", "tmp");
            StreamUtils.copyInputStreamToOutputStream(stream, 
                    new FileOutputStream(tmpFile));
            return tmpFile;
        } else {
            System.out.println("Unable to read the settings file "
                    + "represented by path: '" + filePath + "'");
            return null;
        }
    }

}
