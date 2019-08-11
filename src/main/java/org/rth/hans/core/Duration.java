package org.rth.hans.core;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.TimeZone;

public class Duration {

    private final long value;
    private final ChronoUnit unit;

    // TODO shift to java Duration???
    public Duration(final long value, final ChronoUnit unit) {
        this.value = value;
        this.unit = unit;
    }

    public String toSQLiteFormat() {
        return Long.toString(value) + " " + unit.name();
    }

    public long getValue() {
        return value;
    }

    public ChronoUnit getUnit() {
        return unit;
    }

    // can throw Exception
    public static Duration parseDuration(final String input) {
        final String[] arr = input.split(" +");
        return new Duration(Long.parseLong(arr[0]), ChronoUnit.valueOf(arr[1]));
    }

    private static int toCalendarConstant(final ChronoUnit unit) {
        switch (unit) {
            case MILLIS:
                return Calendar.MILLISECOND;
            case SECONDS:
                return Calendar.SECOND;
            case MINUTES:
                return Calendar.MINUTE;
            case HOURS:
                return Calendar.HOUR;
            case DAYS:
                return Calendar.DAY_OF_MONTH;
            case MONTHS:
                return Calendar.MONTH;
            case YEARS:
                return Calendar.YEAR;
            default:
                throw new IllegalArgumentException("Unsupported ChronoUnit: " + unit);
        }
    }

    private static final TimeZone timeZoneUtc = TimeZone.getTimeZone("UTC");
    public Instant addToInstant(final Instant instant, final long coef) {
        final Calendar calendar = new Calendar.Builder()
                .setInstant(instant.toEpochMilli())
                .setTimeZone(timeZoneUtc)
                .build();
        calendar.add(toCalendarConstant(unit), (int)(value * coef));
        return calendar.toInstant();
    }

    public Instant addToInstant(final Instant instant) {
        return addToInstant(instant, 1L);
    }

    public Instant minusToInstant(final Instant instant) {
        return addToInstant(instant, -1L);
    }

    @Override
    public String toString() {
        return Long.toString(value) + " " + unit.name();
    }

}
