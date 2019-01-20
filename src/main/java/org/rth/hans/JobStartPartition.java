package org.rth.hans;

import java.time.Instant;

public class JobStartPartition {

    private final String jobName;
    private final Instant startPartition;

    public JobStartPartition(final String jobName,
                        final Instant startPartition) {
        this.jobName = jobName;
        this.startPartition = startPartition;
    }

    public String getJobName() {
        return jobName;
    }

    public Instant getStartPartition() {
        return startPartition;
    }
}
