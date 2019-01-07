package org.rth.hans;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public class Hans {

    private static final Logger logger = LoggerFactory.getLogger(Hans.class);

    private static void displayUsageAndExit() {
        logger.error("Usage:");
        logger.error("runScheduler:");
        logger.error("  Hans runScheduler [Configuration file path] [Database file path]");
        logger.error("    [Configuration file path]: Path to the file containing the scheduler configuration");
        logger.error("    [Database file path]: Path to the database file");
        logger.error("parseConfigurationTest:");
        logger.error("  Hans parseConfigurationTest [Configuration file path]");
        logger.error("    [Configuration file path]: Path to the file containing the scheduler configuration");
        System.exit(1);
    }

    public static void main(final String[] args) throws Exception {

        if(args.length >= 1) {
            final String cmd = args[0];
            switch (cmd) {
                case "runScheduler":
                    if(args.length == 3) {
                        final String configurationPath = args[1];
                        final String databasePath = args[2];
                        runScheduler(configurationPath, databasePath);
                    } else {
                        displayUsageAndExit();
                    }
                    break;
                case "parseConfigurationTest":
                    if(args.length == 2) {
                        final String configurationPath = args[1];
                        parseConfigurationTest(configurationPath);
                    } else {
                        displayUsageAndExit();
                    }
                    break;
                default:
                    logger.error("Invalid argument: " + cmd);
                    displayUsageAndExit();
                    break;
            }
        } else {
            displayUsageAndExit();
        }

    }

    private static void parseConfigurationTest(final String configurationPath) throws IOException {
        JobParser.parseJobConfiguration(Utils.readFile(new File(configurationPath)));
        logger.info("`" + configurationPath + "` successfully parsed");
        System.exit(0);
    }

    private static void runScheduler(final String configurationPath, final String databasePath) throws Exception {
        logger.info("Configuration path: " + configurationPath);
        logger.info("Database path: " + databasePath);

        logger.info("Initialisation of the database - Start");
        try(final Database database = new Database(databasePath)) {
            database.resetRunningExecution();
            database.resetJobRunningInstances();
            logger.info("Initialisation of the database - Done");

            logger.info("Start scheduler - Start");
            final Scheduler scheduler = new Scheduler(
                    new JobParser(configurationPath),
                    database
            );
            scheduler.setDaemon(true);
            scheduler.start();
            logger.info("Start scheduler - Done");

            scheduler.join();

            logger.error("Main thread exited... something went wrong...");
        }
        System.exit(1);
    }

}
