package org.rth.hans.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class SchedulerDependencyTests {


    void simpleDependency1Test() throws Exception {
        try (final TemporaryDatabase db = new TemporaryDatabase("temp_database_simple_dependency1_test.db")) {
            Assertions.assertTrue(new Scheduler(
                    new MockResourceJobParser("jobs/simpleDependency1Graph.json"),
                    db
            ).runLoop(1L));
            // activate jobs
            Assertions.assertTrue(db.updateJobIsActivated("job1", true));
            Assertions.assertTrue(db.updateJobIsActivated("job2", true));

            Assertions.assertTrue(new Scheduler(
                    new MockResourceJobParser("jobs/simpleDependency1Graph.json"),
                    db
            ).runLoop(40L));

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


}
