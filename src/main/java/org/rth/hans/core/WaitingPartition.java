package org.rth.hans.core;

import java.time.Instant;
import java.util.Comparator;

public class WaitingPartition {

    private final String jobName;
    private final Instant partition;
    private final Instant activationThreshold;

    public WaitingPartition(final String jobName,
                            final Instant partition,
                            final Instant activationThreshold) {
        this.jobName = jobName;
        this.partition = partition;
        this.activationThreshold = activationThreshold;
    }

    public Instant getPartition() {
        return partition;
    }

    public Instant getActivationThreshold() {
        return activationThreshold;
    }

    public String getJobName() {
        return jobName;
    }

    public static WaitingPartitionComparator comparator = new WaitingPartitionComparator();

    // TODO rename with activationthreshold
    public static class WaitingPartitionComparator implements Comparator<WaitingPartition> {
        @Override
        public int compare(final WaitingPartition wp1,
                           final WaitingPartition wp2) {
            return Long.compare(
                    wp1.getActivationThreshold().toEpochMilli(),
                    wp2.getActivationThreshold().toEpochMilli()
            );
        }
    }

}
