/* $Id$
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *   USA
 */
package dk.netarkivet.deploy2;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import dk.netarkivet.common.utils.Settings;

/** 
 * The application that is run to generate install and start/stop scripts
 * for all physical locations, machines and applications.
 */
public final class DeployApplication {
    static {
        Settings.addDefaultClasspathSettings(
                Constants.BUILD_COMPLETE_SETTINGS_FILE_PATH
        );
    }
    /** The configuration for this deploy. */
    private static DeployConfiguration itConfig;
    /** Argument parameter. */
    private static ArgumentParameters ap = new ArgumentParameters();
    /** The it-config file. */
    private static File itConfigFile;
    /** The NetarchiveSuite file.*/
    private static File netarchiveSuiteFile;
    /** The security policy file.*/
    private static File secPolicyFile;
    /** The log property file.*/
    private static File logPropFile;
    /** The database file.*/
    private static File dbFile;
    /** The arguments for resetting tempDir.*/
    private static boolean resetDirectory;

    /** 
     * The constructor.
     */
    private DeployApplication() { }
    
    /**
     * Run the new deploy.
     * 
     * @param args The Command-line arguments in no particular order:
     * 
     * -C  The it-configuration file (ends with .xml).
     * -Z  The NetarchiveSuite file to be unpacked (ends with .zip).
     * -S  The security policy file (ends with .policy).
     * -L  The logging property file (ends with .prop).
     * -O  [OPTIONAL] The output directory
     * -D  [OPTIONAL] The database
     * -T  [OPTIONAL] The test arguments (httpportoffset, port, 
     *                                    environmentName, mailReciever) 
     */
    public static void main(String[] args) {
        try {
            // Make sure the arguments can be parsed.
            if(!ap.parseParameters(args)) {
                System.err.print(Constants.MSG_ERROR_PARSE_ARGUMENTS);
                System.out.println(ap.listArguments());
                System.exit(0);
            }

            // Check arguments
            if(ap.getCommandLine().getOptions().length 
                    < Constants.ARGUMENTS_REQUIRED) {
                System.err.print(Constants.MSG_ERROR_NOT_ENOUGH_ARGUMENTS);
                System.out.println();
                System.out.println(
                        "Use DeployApplication with following arguments:");
                System.out.println(ap.listArguments());
                System.out.println(
                        "outputdir defaults to "
                        + "./environmentName (set in config file)");
                System.out.println("Example: ");
                System.out.println(
                        "DeployApplication "
                        + "-C./conf/it-config.xml "
                        + "-Z./NetarchiveSuite-1.zip "
                        + "-S./conf/security.policy "
                        + "-L./conf/log.prop");
                System.exit(0);
            }
            // test if more arguments than options is given 
            if (args.length > ap.getOptions().getOptions().size()) {
                System.err.print(
                        Constants.MSG_ERROR_TOO_MANY_ARGUMENTS);
                System.out.println();
                System.out.println("Maximum " 
                        + ap.getOptions().getOptions().size() 
                        + "arguments.");
                System.exit(0);
            }

            // Retrieving the configuration filename
            String itConfigFileName = ap.getCommandLine().getOptionValue(
                    Constants.ARG_CONFIG_FILE);
            // Retrieving the NetarchiveSuite filename
            String netarchiveSuiteFileName = ap.getCommandLine().getOptionValue(
                    Constants.ARG_NETARCHIVE_SUITE_FILE);
            // Retrieving the security policy filename
            String secPolicyFileName = ap.getCommandLine().getOptionValue(
                    Constants.ARG_SECURITY_FILE);
            // Retrieving the log property filename
            String logPropFileName = ap.getCommandLine().getOptionValue(
                    Constants.ARG_LOG_PROPERTY_FILE);
            // Retrieving the output directory name
            String outputDir = ap.getCommandLine().getOptionValue(
                    Constants.ARG_OUTPUT_DIRECTORY);
            // Retrieving the database filename
            String databaseFileName = ap.getCommandLine().getOptionValue(
                    Constants.ARG_DATABASE_FILE);
            // Retrieving the test arguments
            String testArguments = ap.getCommandLine().getOptionValue(
                    Constants.ARG_TEST);
            // Retrieving the reset argument
            String resetArgument = ap.getCommandLine().getOptionValue(
                    Constants.ARG_RESET);
            // Retrieving the evaluate argument
            String evaluateArgument = ap.getCommandLine().getOptionValue(
                    Constants.ARG_EVALUATE);

            // check itConfigFileName and retrieve the file
            initConfigFile(itConfigFileName);
            
            // check netarchiveSuiteFileName and retrieve the file
            initNetarchiveSuiteFile(netarchiveSuiteFileName);

            // check sePolicyFileName and retrieve the file
            initSecPolicyFile(secPolicyFileName);

            // check logPropFileName and retrieve the file
            initLogPropFile(logPropFileName);

            // check database
            initDatabase(databaseFileName);
            
            // check and apply the test arguments
            initTestArguments(testArguments);
            
            // check reset arguments.
            initReset(resetArgument);
            
            // evaluates the config file
            initEvaluate(evaluateArgument);
            
            // Make the configuration based on the input data
            itConfig = new DeployConfiguration(
                    itConfigFile,
                    netarchiveSuiteFile,
                    secPolicyFile,
                    logPropFile,
                    outputDir,
                    dbFile,
                    resetDirectory); 

            // Write the scripts, directories and everything
            itConfig.write();
        } catch (SecurityException e) {
            // This problem should only occur in tests -> thus not err message. 
            System.out.println("SECURITY ERROR: " + e);
        } catch (Exception e) {
            // handle other exceptions?
            System.err.println("DEPLOY APPLICATION ERROR: " + e);
        }
    }
    
    /** 
     * Checks the configuration file argument and retrieves the file.
     * 
     * @param itConfigFileName The configuration file argument.
     */
    private static void initConfigFile(String itConfigFileName) {
        // check whether it-config file name is given as argument
        if(itConfigFileName == null) {
            System.err.print(
                    Constants.MSG_ERROR_NO_CONFIG_FILE_ARG);
            System.out.println();
            System.exit(0);
        }
        // check whether it-config file has correct extensions
        if(!itConfigFileName.endsWith(".xml")) {
            System.err.print(
                    Constants.MSG_ERROR_CONFIG_EXTENSION);
            System.out.println();
            System.exit(0);
        }
        // get the file
        itConfigFile = new File(itConfigFileName);
        // check whether the it-config file exists.
        if(!itConfigFile.exists()) {
            System.err.print(
                    Constants.MSG_ERROR_NO_CONFIG_FILE_FOUND);
            System.out.println();
            System.exit(0);
        }
    }

    /** 
     * Checks the NetarchiveSuite file argument and retrieves the file.
     * 
     * @param netarchiveSuiteFileName The NetarchiveSuite argument.
     */
    private static void initNetarchiveSuiteFile(String 
            netarchiveSuiteFileName) {
        // check whether NetarchiveSuite file name is given as argument
        if(netarchiveSuiteFileName == null) {
            System.err.print(
                    Constants.MSG_ERROR_NO_NETARCHIVESUITE_FILE_ARG);
            System.out.println();
            System.exit(0);
        }
        // check whether the NetarchiveSuite file has correct extensions
        if(!netarchiveSuiteFileName.endsWith(".zip")) {
            System.err.print(
                    Constants.MSG_ERROR_NETARCHIVESUITE_EXTENSION);
            System.out.println();
            System.exit(0);
        }
        // get the file
        netarchiveSuiteFile = new File(netarchiveSuiteFileName);
        // check whether the NetarchiveSuite file exists.
        if(!netarchiveSuiteFile.exists()) {
            System.err.print(
                    Constants.MSG_ERROR_NO_NETARCHIVESUITE_FILE_FOUND);
            System.out.println();
            System.exit(0);
        }
    }
    
    /** 
     * Checks the security policy file argument and retrieves the file.
     * 
     * @param secPolicyFileName The security policy argument.
     */
    private static void initSecPolicyFile(String secPolicyFileName) {
        // check whether security policy file name is given as argument
        if(secPolicyFileName == null) {
            System.err.print(
                    Constants.MSG_ERROR_NO_SECURITY_FILE_ARG);
            System.out.println();
            System.exit(0);
        }
        // check whether security policy file has correct extensions
        if(!secPolicyFileName.endsWith(".policy")) {
            System.err.print(
                    Constants.MSG_ERROR_SECURITY_EXTENSION);
            System.out.println();
            System.exit(0);
        }
        // get the file
        secPolicyFile = new File(secPolicyFileName);
        // check whether the security policy file exists.
        if(!secPolicyFile.exists()) {
            System.err.print(
                    Constants.MSG_ERROR_NO_SECURITY_FILE_FOUND);
            System.out.println();
            System.exit(0);
        }
    }
    
    /** 
     * Checks the log property file argument and retrieves the file.
     * 
     * @param logPropFileName The log property argument.
     */
    private static void initLogPropFile(String logPropFileName) {
        // check whether log property file name is given as argument
        if(logPropFileName == null) {
            System.err.print(
                    Constants.MSG_ERROR_NO_LOG_PROPERTY_FILE_ARG);
            System.out.println();
            System.exit(0);
        }
        // check whether the log property file has correct extensions
        if(!logPropFileName.endsWith(".prop")) {
            System.err.print(
                    Constants.MSG_ERROR_LOG_PROPERTY_EXTENSION);
            System.out.println();
            System.exit(0);
        }
        // get the file
        logPropFile = new File(logPropFileName);
        // check whether the log property file exists.
        if(!logPropFile.exists()) {
            System.err.print(
                    Constants.MSG_ERROR_NO_LOG_PROPERTY_FILE_FOUND);
            System.out.println();
            System.exit(0);
        }
    }
    
    /**
     * Checks the database argument (if any) for extension and existence.
     * 
     * @param databaseFileName The name of the database file.
     */
    private static void initDatabase(String databaseFileName) {
        dbFile = null;
        // check the extension on the database, if it is given as argument 
        if(databaseFileName != null) {
            if(!databaseFileName.endsWith(".jar") 
                    && !databaseFileName.endsWith(".zip")) {
                System.err.print(
                        Constants.MSG_ERROR_DATABASE_EXTENSION);
                System.out.println();
                System.exit(0);
            }
            
            // get the file
            dbFile = new File(databaseFileName);
            // check whether the database file exists.
            if(!dbFile.exists()) {
                System.err.print(
                            Constants.MSG_ERROR_NO_DATABASE_FILE_FOUND);
                System.out.println();
                System.exit(0);
            }
        }
    }
    
    /**
     * Checks the arguments for resetting the directory.
     * Only the arguments 'y' or 'yes' is accepted for resetting 
     * the temporary directory. 
     * 
     * @param resetArgument The argument for resetting given.
     */
    private static void initReset(String resetArgument) {
        if(resetArgument != null) {
            if(resetArgument.equalsIgnoreCase("y")
                    || resetArgument.equalsIgnoreCase("yes")) {
                // if positive argument, then set to true.
                resetDirectory = true;
            } else if (resetArgument.equalsIgnoreCase("n")
                    || resetArgument.equalsIgnoreCase("no")) {
                // if negative argument, then set to false.
                resetDirectory = false;
            } else {
                // if wrong argument, notify and set to false.
                System.err.println(Constants.MSG_ERROR_RESET_ARGUMENT);
                resetDirectory = false;
            }
        } else {
            // if no arguments, then 
            resetDirectory = false;
        }
    }
    
    /**
     * Checks the arguments for evaluating the config file.
     * Only the arguments 'y' or 'yes' is accepted for evaluation.
     * 
     * @param evaluateArgument The argument for evaluation.
     */
    public static void initEvaluate(String evaluateArgument) {
        if(evaluateArgument != null) {
            // check if argument is acknowledgement ('y' or 'yes')
            if(evaluateArgument.equalsIgnoreCase("y")
                    || evaluateArgument.equalsIgnoreCase("yes")) {
                // if yes, then evaluate config file
                EvaluateConfigFile evf = new EvaluateConfigFile(itConfigFile);
                evf.evaluate();
            }
        }
    }
       
    /**
     * Applies the test arguments.
     * 
     * If the test arguments are given correctly, the configuration file is 
     * loaded and changed appropriately, then written to a test configuration 
     * file.
     * 
     * The new test configuration file has the same name as the original 
     * configuration file, except ".xml" is replaced by "_text.xml".
     * Thus "path/config.xml" -> "path/config_test.xml".  
     * 
     * @param testArguments The test arguments.
     */
    private static void initTestArguments(String testArguments) {
        // test if any test arguments (if none, don't apply, just stop).
        if(testArguments == null || testArguments.equalsIgnoreCase("")) {
            return;
        }

        String[] changes = testArguments.split("[,]");
        if(changes.length != Constants.TEST_ARGUMENTS_REQUIRED) {
            System.err.print(
                    Constants.MSG_ERROR_TEST_ARGUMENTS);
            System.exit(0);
        }

        try {
            CreateTestInstance cti = new CreateTestInstance(itConfigFile);

            // apply the arguments
            cti.applyTestArguments(changes[0], changes[1], changes[2], 
                    changes[2+1]); 
            //annoying 3 code-style 'warning' (change maximum acceptable value)

            // replace ".xml" with "_test.xml"
            String tmp = itConfigFile.getPath();
            // split this into two ("path/config", ".xml") 
            String[] configFile = tmp.split("[.]");
            // take the first part and add test ending 
            // ("path/config" + "_test.xml" = "path/config_test.xml")
            String nameOfNewConfig =  configFile[0] 
                    + Constants.TEST_CONFIG_FILE_REPLACE_ENDING;

            // create and use new config file.
            cti.createSettingsFile(nameOfNewConfig);
            itConfigFile = new File(nameOfNewConfig);
        } catch (IOException e) {
            System.out.println("Error in test arguments: " + e);
            System.exit(0);
        }
    }
    
    /**
     * Handles the incoming arguments.
     * 
     */
    private static class ArgumentParameters {
        /** Options object for parameters.*/
        private Options options = new Options();
        /** Parser for parsing the command line arguments.*/
        private CommandLineParser parser = new PosixParser();
        /** The command line.*/
        private CommandLine cmd;
         
        /**
         * Initialise options by setting legal parameters for batch jobs.
         */
        ArgumentParameters() {
            options.addOption(Constants.ARG_CONFIG_FILE, 
                    true, "Config file.");
            options.addOption(Constants.ARG_NETARCHIVE_SUITE_FILE, 
                    true, "The NetarchiveSuite package file.");
            options.addOption(Constants.ARG_SECURITY_FILE, 
                    true, "Security property file.");
            options.addOption(Constants.ARG_LOG_PROPERTY_FILE, 
                    true, "Log property file.");
            options.addOption(Constants.ARG_OUTPUT_DIRECTORY, 
                    true, "[OPTIONAL] output directory.");
            options.addOption(Constants.ARG_DATABASE_FILE, 
                    true, "[OPTIONAL] Database file.");
            options.addOption(Constants.ARG_TEST, 
                    true, "[OPTIONAL] Tests arguments (offset for http port, "
                    + "http port, environment name, mail receiver).");
            options.addOption(Constants.ARG_RESET,
                    true, "[OPTIONAL] Reset temp directory (y/n - anything "
                    + "other than 'y' or 'yes' is asserted no).");
            options.addOption(Constants.ARG_EVALUATE, true, "[OPTIONAL] "
                    + "Evaluate the config file.");
        }

        /**
         * Parsing the input arguments.
         * 
         * @param args The input arguments.
         * @return Whether it parsed correctly or not.
         */
        Boolean parseParameters(String[] args) {
            try {
                // parse the command line arguments
                cmd = parser.parse(options, args);
            } catch(ParseException exp) {
                return false;
            }
            return true;
        }
        
        /**
         * Get the list of possible arguments with their description.
         * 
         * @return The list describing the possible arguments.
         */
        String listArguments() {
            StringBuilder res = new StringBuilder("\n");
            res.append("Arguments:");
            // add options
            for (Object o: options.getOptions()) {
                Option op = (Option) o;
                res.append("\n");
                res.append("-");
                res.append(op.getOpt());
                res.append(" ");
                res.append(op.getDescription());
            }
            return res.toString();
        }
        
        /**
         * For retrieving the options.
         * 
         * @return The options.
         */
        public Options getOptions() {
            return options;
        }
        
        /**
         * For retrieving the commandLine.
         * 
         * @return The cmd.
         */
        public CommandLine getCommandLine() {
            return cmd;
        }
    }
}
