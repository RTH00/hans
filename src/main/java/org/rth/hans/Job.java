package org.rth.hans;

import java.time.Instant;

public class Job {

    enum FailureBehavior {
        RETRY,
        MARK_SUCCESS
    }

    private final String name;
    private final Instant startPartition;
    private final Instant endPartition;
    private final Duration increment;
    private final String[] commands;
    private final Dependency[] dependencies;
    private final long maxParallelism;
    private final FailureBehavior failureBehavior;
    private final Duration retryDelay;
    private final Duration retention; // in seconds
    private final String stdoutPath;
    private final String stderrPath;

    public Job(final String name,
               final Instant startPartition,
               final Instant endPartition,
               final Duration increment,
               final String[] commands,
               final Dependency[] dependencies,
               final long maxParallelism,
               final FailureBehavior failureBehavior,
               final Duration retryDelay,
               final Duration retention,
               final String stdoutPath,
               final String stderrPath) {
        this.name = name;
        this.startPartition = startPartition;
        this.endPartition = endPartition;
        this.increment = increment;
        this.commands = commands;
        this.dependencies = dependencies;
        this.maxParallelism = maxParallelism;
        this.failureBehavior = failureBehavior;
        this.retryDelay = retryDelay;
        this.retention = retention;
        this.stdoutPath = stdoutPath;
        this.stderrPath = stderrPath;
    }

    public String getName() {
        return name;
    }

    public Instant getStartPartition() {
        return startPartition;
    }

    public Instant getEndPartition() {
        return endPartition;
    }

    public Duration getIncrement() {
        return increment;
    }

    public String[] getCommands() {
        return commands;
    }

    public Dependency[] getDependencies() {
        return dependencies;
    }

    public long getMaxParallelism() {
        return maxParallelism;
    }

    public FailureBehavior getFailureBehavior() {
        return failureBehavior;
    }

    public Duration getRetryDelay() {
        return retryDelay;
    }

    public Duration getRetention() {
        return retention;
    }

    public String getStdoutPath() {
        return stdoutPath;
    }

    public String getStderrPath() {
        return stderrPath;
    }
}
