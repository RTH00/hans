package org.rth.hans.ui;

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.Interceptor;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class TimerInterceptor implements Interceptor {

    public static class Aggregator {

        private final AtomicLong sumValue;
        private final AtomicLong sumCount;

        public Aggregator() {
            sumValue = new AtomicLong(0L);
            sumCount = new AtomicLong(0L);
        }

        public void addValue(final long value) {
            sumValue.addAndGet(value);
            sumCount.addAndGet(1L);
        }

        public long getSumValue() {
            return sumValue.get();
        }
        public long getSumCount() {
            return sumCount.get();
        }
        public double getAverage() {
            return (double)sumValue.get() / (double)sumCount.get();
        }

    }

    private static final ConcurrentHashMap<String, Aggregator> latencies = new ConcurrentHashMap<>();

    @Override
    public void destroy() {
        latencies.clear();
    }

    @Override
    public void init() {

    }

    // /!\ concurrent calls with interceptor
    @Override
    public String intercept(final ActionInvocation actionInvocation) throws Exception {
        final String key = actionInvocation.getAction().getClass().getCanonicalName();
        final Instant start = Instant.now();
        final String result = actionInvocation.invoke();
        final Instant stop = Instant.now();
        addLatency(key, stop.toEpochMilli() - start.toEpochMilli());
        return result;
    }

    private static void addLatency(final String key, final long value) {
        final Aggregator aggregator = latencies.computeIfAbsent(key, str -> new Aggregator());
        aggregator.addValue(value);
    }

    public static ConcurrentHashMap<String, Aggregator> getLatencies() {
        return latencies;
    }
}
