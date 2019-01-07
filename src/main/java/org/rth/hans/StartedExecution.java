package org.rth.hans;

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
    private final Process process;

    public StartedExecution(final String jobName,
                            final Instant partition,
                            final Process process /* nullable */) {
        this.jobName = jobName;
        this.partition = partition;
        this.process = process;
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

}
