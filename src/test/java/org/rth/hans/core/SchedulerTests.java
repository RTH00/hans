package org.rth.hans.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

// TODO split scheduler tests
public class SchedulerTests {

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
            ).runLoop(1L));
            Assertions.assertTrue(db.updateJobIsActivated("job1", true));
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
    void previousStartPartitionTest() throws Exception {

        try (final TemporaryDatabase db = new TemporaryDatabase("temp_database_previous_start_partition_test.db")) {
            Assertions.assertTrue(new Scheduler(
                    new MockResourceJobParser("jobs/previousStartPartition1Graph.json"),
                    db
            ).runLoop(1L));
            Assertions.assertEquals(1, db.getAllExecutions().length);
            Assertions.assertTrue(new Scheduler(
                    new MockResourceJobParser("jobs/previousStartPartition2Graph.json"),
                    db
            ).runLoop(1L));
            Assertions.assertEquals(48, db.getAllExecutions().length);
        }

    }

    @Test
    void shiftLaterStartPartitionTest() throws Exception {

        try (final TemporaryDatabase db = new TemporaryDatabase("temp_database_shift_later_start_partition_test.db")) {
            Assertions.assertTrue(new Scheduler(
                    new MockResourceJobParser("jobs/shiftLaterStartPartition1Graph.json"),
                    db
            ).runLoop(1L));
            Assertions.assertEquals(24, db.getAllExecutions().length);
            Assertions.assertTrue(new Scheduler(
                    new MockResourceJobParser("jobs/shiftLaterStartPartition2Graph.json"),
                    db
            ).runLoop(1L));
            Assertions.assertEquals(48, db.getAllExecutions().length);
        }

    }

    @Test
    void sameEndPartitionTest() throws Exception {
        try (final TemporaryDatabase db = new TemporaryDatabase("temp_database_same_end_partition_test.db")) {
            Assertions.assertTrue(new Scheduler(
                    new MockResourceJobParser("jobs/sameEndPartitionGraph.json"),
                    db
            ).runLoop(3L));
            final Execution[] executions = db.getAllExecutions();
            Assertions.assertEquals(0, executions.length);
        }
    }

}