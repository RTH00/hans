package org.rth.hans;

public class Dependency {

    private final String jobName;
    private final Duration shift;

    public Dependency(final String jobName,
                      final Duration shift) {
        this.jobName = jobName;
        this.shift = shift;
    }

    public String getJobName() {
        return jobName;
    }

    public Duration getShift() {
        return shift;
    }
}
