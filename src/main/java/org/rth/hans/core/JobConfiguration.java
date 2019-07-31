package org.rth.hans.core;

public class JobConfiguration {

    public static final String MAX_PARALLELISM_KEY = "MAX_PARALLELISM_KEY";
    public static final String SCHEDULER_POLLING_TIME_KEY = "SCHEDULER_POLLING_TIME_KEY";
    public static final String NEW_CONFIGURATION_POLLING_TIME_KEY = "NEW_CONFIGURATION_POLLING_TIME_KEY";

    private final long maxParallelism;
    private final Duration schedulerPollingTime;
    private final Duration newConfigurationPollingTime;

    private final Job[] jobs;

    public JobConfiguration(
            final long maxParallelism,
            final Duration schedulerPollingTime,
            final Duration newConfigurationPollingTime,
            final Job[] jobs) {
        this.maxParallelism = maxParallelism;
        this.schedulerPollingTime = schedulerPollingTime;
        this.newConfigurationPollingTime = newConfigurationPollingTime;
        this.jobs = jobs;
    }


    public long getMaxParallelism() {
        return maxParallelism;
    }

    public Duration getSchedulerPollingTime() {
        return schedulerPollingTime;
    }

    public Duration getNewConfigurationPollingTime() {
        return newConfigurationPollingTime;
    }

    public Job[] getJobs() {
        return jobs;
    }
}
