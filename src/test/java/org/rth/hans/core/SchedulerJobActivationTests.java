package org.rth.hans.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class SchedulerJobActivationTests {

    @Test
    void jobActivationTest() throws Exception {
        try (final TemporaryDatabase db = new TemporaryDatabase("temp_database_job_activation_test.db")) {
            // TODO replace with a load of the conf
            Assertions.assertTrue(new Scheduler(
                    new MockResourceJobParser("jobs/jobActivationGraph.json"),
                    db
            ).runLoop(1L));
            Assertions.assertTrue(db.updateJobIsActivated("job1", false));
            Assertions.assertTrue(db.updateJobIsActivated("job2", true));
            Assertions.assertTrue(new Scheduler(
                    new MockResourceJobParser("jobs/jobActivationGraph.json"),
                    db
            ).runLoop(10L));

            final Execution[] executions = db.getAllExecutions();
            Assertions.assertEquals(24L,
                    Arrays.stream(executions)
                            .filter(exe -> "job1".equals(exe.getJobName()) && exe.getStatus() == StartedExecution.Status.INITIALISED)
                            .count()
            );
            Assertions.assertEquals(24L,
                    Arrays.stream(executions)
                            .filter(exe -> "job2".equals(exe.getJobName()) && exe.getStatus() == StartedExecution.Status.SUCCESS)
                            .count()
            );
        }
    }

}
