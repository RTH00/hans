package org.rth.hans;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class SchedulerTests {

    @Test
    void simpleDependency1Test() throws Exception {
        try (final TemporaryDatabase db = new TemporaryDatabase("temp_database_simple_dependency1_test.db")) {
            final Scheduler scheduler = new Scheduler(
                    new MockResourceJobParser("jobs/simpleDependency1Graph.json"),
                    db
            );
            Assertions.assertTrue(scheduler.runLoop(40L));
            final Execution[] executions = db.getAllExecutions();
            Assertions.assertEquals(24, Arrays.stream(executions)
                    .filter(exe -> "job1".equals(exe.getJobName()))
                    .filter(exe -> StartedExecution.Status.SUCCESS == exe.getStatus())
                    .map(Execution::getPartition)
                    .distinct()
                    .count());
            Assertions.assertEquals(23, Arrays.stream(executions)
                    .filter(exe -> "job2".equals(exe.getJobName()))
                    .filter(exe -> StartedExecution.Status.SUCCESS == exe.getStatus())
                    .map(Execution::getPartition)
                    .distinct()
                    .count());

            Assertions.assertTrue(Arrays.stream(executions)
                    .filter(exe -> "job2".equals(exe.getJobName()))
                    .filter(exe -> StartedExecution.Status.SUCCESS == exe.getStatus())
                    .anyMatch(exe -> "2000-01-01 01:00:00".equals(Utils.toSqliteFormat(exe.getPartition()))));
            Assertions.assertTrue(Arrays.stream(executions)
                    .filter(exe -> "job2".equals(exe.getJobName()))
                    .filter(exe -> StartedExecution.Status.INITIALISED == exe.getStatus())
                    .anyMatch(exe -> "2000-01-01 00:00:00".equals(Utils.toSqliteFormat(exe.getPartition()))));
        }
    }

    @Test
    void applyRetention1Test() throws Exception {
        try (final TemporaryDatabase db = new TemporaryDatabase("temp_database_apply_retention_1_test.db")) {
            final Scheduler scheduler = new Scheduler(
                    new MockResourceJobParser("jobs/applyRetention1Graph.json"),
                    db
            );
            Assertions.assertTrue(scheduler.runLoop(1L));
            final Execution[] executions = db.getAllExecutions();
            Assertions.assertEquals(7, executions.length);
        }
    }

    @Test
    void buildNextPartitionIndex1Test() throws Exception {
        try (final TemporaryDatabase db = new TemporaryDatabase("temp_database_build_next_partition_index_1_test.db")) {
            Assertions.assertTrue(new Scheduler(
                    new MockResourceJobParser("jobs/buildNextPartitionIndex1Graph.json"),
                    db
            ).runLoop(10L));
            db.resetJobRunningInstances();
            db.resetRunningExecution();
            Assertions.assertTrue(new Scheduler(
                    new MockResourceJobParser("jobs/buildNextPartitionIndex1BisGraph.json"),
                    db
            ).runLoop(10L));
            final Execution[] executions = db.getAllExecutions();
            Assertions.assertArrayEquals(
                    new String[]{"2000-01-01 00:00:00", "2000-01-20 00:00:00"},
                    Arrays.stream(executions)
                            .filter(exe -> "job1".equals(exe.getJobName()))
                            .map(exe -> Utils.toSqliteFormat(exe.getPartition()))
                            .sorted()
                            .toArray(String[]::new)
            );
        }
    }

    @Test
    void buildNextPartitionIndex2Test() throws Exception {
        try (final TemporaryDatabase db = new TemporaryDatabase("temp_database_build_next_partition_index_2_test.db")) {
            Assertions.assertTrue(new Scheduler(
                    new MockResourceJobParser("jobs/buildNextPartitionIndex2Graph.json"),
                    db
            ).runLoop(20L));

            {
                final long count = Arrays.stream(db.getAllExecutions())
                        .filter(exe -> "job1".equals(exe.getJobName()))
                        .filter(exe -> exe.getStatus() == StartedExecution.Status.SUCCESS)
                        .count();
                Assertions.assertTrue(count <= 20);
                Assertions.assertTrue(count >= 10, "count: " + count);
            }

            db.resetJobRunningInstances();
            db.resetRunningExecution();

            Assertions.assertTrue(new Scheduler(
                    new MockResourceJobParser("jobs/buildNextPartitionIndex2BisGraph.json"),
                    db
            ).runLoop(20L));

            {
                final Execution[] executions = db.getAllExecutions();
                Assertions.assertArrayEquals(
                        new String[]{
                                "2000-01-01 00:00:00", "2000-01-02 00:00:00", "2000-01-03 00:00:00", "2000-01-04 00:00:00", "2000-01-05 00:00:00",
                                "2000-01-06 00:00:00", "2000-01-07 00:00:00", "2000-01-08 00:00:00", "2000-01-09 00:00:00", "2000-01-10 00:00:00",
                                "2000-01-11 00:00:00", "2000-01-12 00:00:00", "2000-01-13 00:00:00", "2000-01-14 00:00:00", "2000-01-15 00:00:00",
                                "2000-01-16 00:00:00", "2000-01-17 00:00:00", "2000-01-18 00:00:00", "2000-01-19 00:00:00", "2000-01-20 00:00:00",
                                "2000-01-21 00:00:00", "2000-01-22 00:00:00", "2000-01-23 00:00:00", "2000-01-24 00:00:00", "2000-01-25 00:00:00",
                                "2000-01-26 00:00:00", "2000-01-27 00:00:00", "2000-01-28 00:00:00", "2000-01-29 00:00:00", "2000-01-30 00:00:00",
                                "2000-01-31 00:00:00"
                        },
                        Arrays.stream(executions)
                                .filter(exe -> "job1".equals(exe.getJobName()))
                                .filter(exe -> exe.getStatus() == StartedExecution.Status.SUCCESS)
                                .map(exe -> Utils.toSqliteFormat(exe.getPartition()))
                                .sorted()
                                .toArray(String[]::new)
                );
            }
        }
    }


    @AfterAll
    static void cleanLogs() {
        Assertions.assertTrue(new File("temp_all_job_test_scheduler_logs.txt").delete(), "Fail to remove test logs");
    }

    @Test
    void applyPartitionTemplating1Test() throws Exception {
        final ArrayList<String> cmds = new ArrayList<>();
        cmds.add("blabla");
        cmds.add("");
        cmds.add(" bla [partition]");
        cmds.add("bla [[partition]]");
        Assertions.assertArrayEquals(
                new String[]{"blabla", "", " bla [partition]", "bla 2018-12-24 01:23:45"},
                Scheduler.applyPartitionTemplating(
                        cmds,
                        Utils.parseSqliteFormat("2018-12-24 01:23:45")).toArray(new String[0])
        );
    }

    @Test
    void applyPartitionTemplating2Test() throws Exception {
        Assertions.assertEquals(
                "blabla2018-12-24 01:23:45   blabla[2018-12-24 01:23:45]   dkhusefipsef",
                Scheduler.applyPartitionTemplating(
                        "blabla[[partition]]   blabla[[[partition]]]   dkhusefipsef",
                        Utils.parseSqliteFormat("2018-12-24 01:23:45")
                )
        );
    }

    @Test
    void failStartWithoutValidConfigurationTest() throws Exception {
        try (final TemporaryDatabase db = new TemporaryDatabase("temp_database_fail_start_without_configuration_test.db")) {
            Assertions.assertFalse(new Scheduler(
                    new MockResourceJobParser("jobs/invalidGraph.json"),
                    db
            ).runLoop(3L));
        }
    }

    @Test
    void minimalValidGraphTest() throws Exception {
        try (final TemporaryDatabase db = new TemporaryDatabase("temp_database_minimal_valid_graph_test.db")) {
            Assertions.assertTrue(new Scheduler(
                    new MockResourceJobParser("jobs/minimalValidGraph.json"),
                    db
            ).runLoop(3L));
        }
    }

    @Test
    void emptyCommandTest() throws Exception {
        try (final TemporaryDatabase db = new TemporaryDatabase("temp_database_empty_command_test.db")) {
            Assertions.assertFalse(new Scheduler(
                    new MockResourceJobParser("jobs/emptyCommandGraph.json"),
                    db
            ).runLoop(1L));
        }
    }

    @Test
    void invalidCommandTest() throws Exception {
        try (final TemporaryDatabase db = new TemporaryDatabase("temp_database_invalid_command_test.db")) {
            Assertions.assertTrue(new Scheduler(
                    new MockResourceJobParser("jobs/invalidCommandGraph.json"),
                    db
            ).runLoop(10L));

            final Execution[] executions = db.getAllExecutions();
            Assertions.assertTrue(executions.length == 1);
            final Execution execution = executions[0];
            Assertions.assertEquals("2000-01-01 00:00:00", Utils.toSqliteFormat(execution.getPartition()));
            Assertions.assertEquals(StartedExecution.Status.FAILURE, execution.getStatus());
        }
    }

    @Test
    void maxParallelism1Test() throws Exception {
        maxParallelismTest(3L, 1);
    }

    @Test
    void maxParallelism2Test() throws Exception {
        maxParallelismTest(4L, 2);
    }

    void maxParallelismTest(final long expectedRunning, final int nbr) throws Exception {
        try (final TemporaryDatabase db = new TemporaryDatabase("temp_database_max_parallelism_" + nbr + "_test.db")) {
            Assertions.assertTrue(new Scheduler(
                    new MockResourceJobParser("jobs/maxParallelism" + nbr + "Graph.json"),
                    db
            ).runLoop(1L));
            final Execution[] executions = db.getAllExecutions();
            Assertions.assertEquals(24L,
                    Arrays.stream(executions)
                            .filter(exe -> StartedExecution.Status.INITIALISED.equals(exe.getStatus())
                                    || StartedExecution.Status.RUNNING.equals(exe.getStatus()))
                            .count()
            );
            Assertions.assertEquals(expectedRunning,
                    Arrays.stream(executions)
                            .filter(exe -> StartedExecution.Status.RUNNING.equals(exe.getStatus()))
                            .count()
            );
        }
    }

}