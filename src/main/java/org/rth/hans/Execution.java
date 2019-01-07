package org.rth.hans;

import java.time.Instant;

public class Execution {

    private final String jobName;
    private final Instant partition;
    private final StartedExecution.Status status;
    private final Instant startTime;
    private final Instant endTime;
    private final Instant nextScheduleTime;

    public Execution(final String jobName,
                     final Instant partition,
                     final StartedExecution.Status status,
                     final Instant startTime,
                     final Instant endTime,
                     final Instant nextScheduleTime) {
        this.jobName = jobName;
        this.partition = partition;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.nextScheduleTime = nextScheduleTime;
    }


    public String getJobName() {
        return jobName;
    }

    public Instant getPartition() {
        return partition;
    }

    public StartedExecution.Status getStatus() {
        return status;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public Instant getNextScheduleTime() {
        return nextScheduleTime;
    }
}
