package org.rth.hans.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class Hans implements ServletContextListener {

    private static final Logger logger = LogManager.getLogger(Hans.class);

    public static Database database = null;


    private static String checkEnv(final String key) {
        final String value = System.getenv(key);
        if(value == null) {
            logger.error(key + " is not set in the env");
            System.exit(1);
            return null; // not executed
        } else {
            return value;
        }
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("Starting Hans...");
        final String databasePath = checkEnv("HANS_DATABASE_PATH");
        final String configurationPath = checkEnv("HANS_CONFIGURATION_PATH");

        logger.info("database path: " + databasePath);
        logger.info("configuration path: " + configurationPath);
        try {
            database = new Database(databasePath);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        try {
            final Thread thread = new Thread(new SchedulerRunner(configurationPath));
            thread.setName("Scheduler");
            thread.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        logger.info("Closing database connection...");
        try {
            if(database != null) {
                database.close();
            }
        } catch (final Exception e) {
            logger.catching(e);
        }
    }

    private static class SchedulerRunner implements Runnable {

        private final String configurationPath;

        public SchedulerRunner(final String configurationPath) {
            this.configurationPath = configurationPath;
        }

        @Override
        public void run() {
            try {
                runScheduler(configurationPath);
            } catch (Exception e) {
                logger.catching(e);
                System.exit(1);
            }
        }
    }

    private static void runScheduler(final String configurationPath) throws Exception {
        logger.info("Configuration path: " + configurationPath);

        logger.info("Initialisation of the database - Start");
        database.resetRunningExecution();
        database.resetJobRunningInstances();
        logger.info("Initialisation of the database - Done");

        logger.info("Start scheduler - Start");
        final Scheduler scheduler = new Scheduler(
                new JobParser(configurationPath),
                database
        );
        scheduler.setDaemon(true); // TODO keep?
        scheduler.start();
        logger.info("Start scheduler - Done");

        scheduler.join();

        logger.error("Main thread exited... something went wrong...");
        System.exit(1);
    }

}
