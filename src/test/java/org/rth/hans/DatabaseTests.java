package org.rth.hans;

import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.*;

import java.io.File;
import java.time.temporal.ChronoUnit;

class DatabaseTests {

    @Test
    void databaseCreatedCorrectlyTest() throws Exception {
        final String dbFileName = "temp_database_creation_test.db";
        final File dbFile = new File(dbFileName);
        new Database(dbFile.getAbsolutePath()).close();
        Assertions.assertEquals(true, dbFile.delete(),"Fail to remove temp database: " + dbFile.getAbsolutePath());
    }

    @Test
    void jobImportTest() throws Exception {
        try(final TemporaryDatabase db = new TemporaryDatabase("temp_database_job_import_test.db")) {
            db.pushConfiguration(new JobConfiguration(
                    8,
                    new Duration(1234, ChronoUnit.MILLIS),
                    new Duration(2345, ChronoUnit.MILLIS),
                    new Job[]{new Job(
                            "jobNameTest",
                            Utils.parseSqliteFormat("2000-01-01 00:00:00"),
                            Utils.parseSqliteFormat("2001-01-01 00:00:00"),
                            new Duration(3601, ChronoUnit.SECONDS),
                            new String[]{"/bin/echo", "toto"},
                            new Dependency[]{new Dependency("tata", new Duration(3, ChronoUnit.MONTHS))},
                            258,
                            Job.FailureBehavior.RETRY,
                            new Duration(3602, ChronoUnit.SECONDS),
                            new Duration(1, ChronoUnit.DAYS),
                            "/something/stdout",
                            "/something/stderr"
                    )}));
            // general conf
            Assertions.assertEquals(8, Long.parseLong(db.getConfiguration(JobConfiguration.MAX_PARALLELISM_KEY)));
            Assertions.assertEquals("1234 MILLIS", db.getConfiguration(JobConfiguration.SCHEDULER_POLLING_TIME_KEY));
            Assertions.assertEquals("2345 MILLIS", db.getConfiguration(JobConfiguration.NEW_CONFIGURATION_POLLING_TIME_KEY));
            // job
            final Job fromDb = db.getJob("jobNameTest");
            Assertions.assertEquals("2000-01-01 00:00:00", Utils.toSqliteFormat(fromDb.getStartPartition()));
            Assertions.assertEquals("2001-01-01 00:00:00", Utils.toSqliteFormat(fromDb.getEndPartition()));
            Assertions.assertEquals(3601L, fromDb.getIncrement().getValue());
            Assertions.assertEquals(ChronoUnit.SECONDS, fromDb.getIncrement().getUnit());
            Assertions.assertArrayEquals(db.getJobCommands("jobNameTest").toArray(new String[0]), new String[]{"/bin/echo", "toto"});

            Assertions.assertEquals(258L, fromDb.getMaxParallelism());
            Assertions.assertEquals(Job.FailureBehavior.RETRY, fromDb.getFailureBehavior());
            Assertions.assertEquals(3602L, fromDb.getRetryDelay().getValue());
            Assertions.assertEquals(ChronoUnit.SECONDS, fromDb.getRetryDelay().getUnit());
            Assertions.assertEquals(1L, fromDb.getRetention().getValue());
            Assertions.assertEquals(ChronoUnit.DAYS, fromDb.getRetention().getUnit());
            Assertions.assertEquals("/something/stdout", fromDb.getStdoutPath());
            Assertions.assertEquals("/something/stderr", fromDb.getStderrPath());
        }
    }
}