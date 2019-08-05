package org.rth.hans.ui;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.opensymphony.xwork2.Action.SUCCESS;

public class Performance {

    public static class PerformanceInfo {

        private static final DecimalFormat timeFormatter = new DecimalFormat("0.00");

        private final String key;
        private final String averageTime;
        private final String count;
        private final String totalTime;

        public PerformanceInfo(final String key, final TimerInterceptor.Aggregator aggregator) {
            this.key = key;
            this.averageTime = timeFormatter.format(aggregator.getAverage());
            this.count = Long.toString(aggregator.getSumCount());
            this.totalTime = timeFormatter.format(aggregator.getSumValue());
        }

        public String getKey() {
            return key;
        }

        public String getAverageTime() {
            return averageTime;
        }

        public String getCount() {
            return count;
        }

        public String getTotalTime() {
            return totalTime;
        }

        public static final PerformanceInfoCaseInsensitiveKeyComparator performanceInfoCaseInsensitiveKeyComparator =
                new PerformanceInfoCaseInsensitiveKeyComparator();

        public static class PerformanceInfoCaseInsensitiveKeyComparator implements Comparator<PerformanceInfo> {

            @Override
            public int compare(final PerformanceInfo pi1, final PerformanceInfo pi2) {
                return String.CASE_INSENSITIVE_ORDER.compare(pi1.getKey(), pi2.getKey());
            }
        }

    }

    private List<PerformanceInfo> performanceInfos;

    public String execute() throws Exception {
        final ConcurrentHashMap<String, TimerInterceptor.Aggregator> latencies = TimerInterceptor.getLatencies();
        performanceInfos = latencies.entrySet().stream()
                .map(entry -> new PerformanceInfo(entry.getKey(), entry.getValue()))
                .sorted(PerformanceInfo.performanceInfoCaseInsensitiveKeyComparator)
                .collect(Collectors.toList());
        return SUCCESS;
    }

    public List<PerformanceInfo> getPerformanceInfos() {
        return performanceInfos;
    }
}
