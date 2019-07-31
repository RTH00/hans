package org.rth.hans.core;

import java.time.Instant;

public class StartedExecution {

    public enum Status {
        INITIALISED,
        RUNNING,
        SUCCESS,
        FAILURE
    }

    private final String jobName;
    private final Instant partition;
    private final Process process; /* nullable */
    private final Job.FailureBehavior failureBehavior;
    private final Duration retryDelay;

    public StartedExecution(final String jobName,
                            final Instant partition,
                            final Process process,
                            final Job.FailureBehavior failureBehavior,
                            final Duration retryDelay) {
        this.jobName = jobName;
        this.partition = partition;
        this.process = process;
        this.failureBehavior = failureBehavior;
        this.retryDelay = retryDelay;
    }

    public boolean isAlive() {
        if(process == null) {
            return false;
        } else {
            return process.isAlive();
        }
    }

    public boolean isSuccessfull() {
        if(process == null) {
            return false;
        } else {
            return process.exitValue() == 0;
        }
    }

    public String getJobName() {
        return jobName;
    }

    public Instant getPartition() {
        return partition;
    }

    public Job.FailureBehavior getFailureBehavior() {
        return failureBehavior;
    }

    public Duration getRetryDelay() {
        return retryDelay;
    }

}
