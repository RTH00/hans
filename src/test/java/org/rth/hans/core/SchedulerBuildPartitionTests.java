package org.rth.hans.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class SchedulerBuildPartitionTests {


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
            ).runLoop(1L));
            Assertions.assertTrue(db.updateJobIsActivated("job1", true));
            Assertions.assertTrue(new Scheduler(
                    new MockResourceJobParser("jobs/buildNextPartitionIndex2Graph.json"),
                    db
            ).runLoop(20L));

            {
                final long count = Arrays.stream(db.getAllExecutions())
                        .filter(exe -> "job1".equals(exe.getJobName()))
                        .filter(exe -> exe.getStatus() == StartedExecution.Status.SUCCESS)
                        .count();
                Assertions.assertTrue(count <= 20, "count: " + count);
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


}
