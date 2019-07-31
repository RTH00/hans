package org.rth.hans.core;

import java.time.Instant;

public class AvailableExecution {

    private final String jobName;
    private final Instant partition;

    public AvailableExecution(final String jobName, final Instant partition) {
        this.jobName = jobName;
        this.partition = partition;
    }

    public String getJobName() {
        return jobName;
    }

    public Instant getPartition() {
        return partition;
    }
}
