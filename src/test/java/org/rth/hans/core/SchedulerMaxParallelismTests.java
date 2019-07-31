package org.rth.hans.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class SchedulerMaxParallelismTests {

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
            // TODO replace with a load of the conf
            Assertions.assertTrue(new Scheduler(
                    new MockResourceJobParser("jobs/maxParallelism" + nbr + "Graph.json"),
                    db
            ).runLoop(1L));
            Assertions.assertTrue(db.updateJobIsActivated("job1", true));
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
