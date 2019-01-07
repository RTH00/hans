package org.rth.hans;

import java.time.Instant;

public class JobPartition {

    private final String jobName;
    private final Instant startPartition;
    private final Instant lastPartition;

    public JobPartition(final String jobName,
                        final Instant startPartition,
                        final Instant lastPartition) {
        this.jobName = jobName;
        this.startPartition = startPartition;
        this.lastPartition = lastPartition;
    }

    public String getJobName() {
        return jobName;
    }

    public Instant getStartPartition() {
        return startPartition;
    }

    public Instant getLastPartition() {
        return lastPartition;
    }
}
