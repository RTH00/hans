package org.rth.hans;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.*;
import java.io.*;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;

public class JobParser {

    static class JobParserException extends RuntimeException {
        public JobParserException(final String message) {
            super(message);
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(JobParser.class);

    private final String jobPath;

    public JobParser(final String jobPath) {
        this.jobPath = jobPath;
    }

    public JobConfiguration parseJobConfiguration() {
        final File file = new File(jobPath);
        if(file.exists()) {
            try {
                logger.info("New job configuration detected, start parsing...");
                return parseJobConfiguration(Utils.readFile(file).trim());
            } catch (final JobParserException e) {
                logger.error("Error while parsing the job description file: " + e.getMessage());
                return null;
            } catch (final Exception e) {
                logger.error("Error while parsing the job description file.", e);
                return null;
            }
        } else {
            return null;
        }
    }

    public void markAsImported() {
        if(jobPath != null) { // true in tests
            final File file = new File(jobPath);
            if (!file.renameTo(new File(file.getAbsolutePath() + "_IMPORTED"))) {
                logger.warn("Can't rename " + file.getAbsolutePath() + " this may lead to scheduler performance issues");
            }
        }
    }

    public static void sanityCheck(final Job[] jobs) {
        final HashSet<String> jobNames = new HashSet<>();
        for(final Job job: jobs) {
            if(!jobNames.add(job.getName())) {
                throw new JobParserException("Job name: `" + job.getName() + "` is already used");
            }
        }
        for(final Job job: jobs) {
            for(final Dependency dependency: job.getDependencies()) {
                if(!jobNames.contains(dependency.getJobName())) {
                    throw new JobParserException("Dependency on `" + dependency.getJobName() + "`is invalid, this job name is invalid");
                }
            }
        }
    }

    public static JobConfiguration parseJobConfiguration(final String input) {
        final JsonValue value;
        try {
            value = Json.createReader(new StringReader(input)).readValue();
        } catch (final JsonException e) {
            throw new JobParserException("Job configuration should be a valid Json, parser error: " + e.getMessage());
        }
        if(value.getValueType() != JsonValue.ValueType.OBJECT) {
            throw new JobParserException("Job configuration should be a JsonObject");
        }
        final JsonObject jo = value.asJsonObject();
        return new JobConfiguration(
                parsePositiveLongField(jo, "max_parallelism"),
                parseDurationField(jo, "scheduler_polling_time", true),
                parseDurationField(jo, "new_configuration_polling_time", true),
                parseJobsField(jo, "jobs")
        );
    }

    public static Job[] parseJobsField(final JsonObject jo, final String field) {
        final JsonArray array = parseJsonArray(jo, field);
        final Job[] jobs = new Job[array.size()];
        for(int i = 0; i < jobs.length; i++) {
            final JsonValue value = array.get(i);
            if(value.getValueType() != JsonValue.ValueType.OBJECT) {
               throw new JobParserException("A job should be a JsonObject");
            }
            jobs[i] = parseJob(value.asJsonObject());
        }
        sanityCheck(jobs);
        return jobs;
    }

    private static Job parseJob(final JsonObject json) {
        return new Job(
                parseStringField(json, "name"),
                parseSQLiteFormatPartitionField(json, "start_partition"),
                parseSQLiteFormatPartitionField(json, "end_partition"),
                parseDurationField(json, "increment", true),
                parseNonEmptyStringArray(json, "command"),
                parseDependencyArray(json, "dependencies"),
                parsePositiveLongField(json, "max_parallelism"),
                parseFailureBehavior(json, "on_failure"),
                parseDurationField(json, "retry_delay", true),
                parseDurationField(json, "retention", true),
                parseStringField(json, "stdout_path"),
                parseStringField(json, "stderr_path")
        );
    }

    static JsonValue parseField(final JsonObject jo, final String fieldName) {
        final JsonValue ret = jo.get(fieldName);
        if(ret == null) {
            throw new JobParserException("Can't find `" + fieldName + "` field");
        } else {
            return ret;
        }
    }

    static String parseStringField(final JsonObject jo, final String fieldName) {
        final JsonValue value = parseField(jo, fieldName);
        if(value.getValueType() == JsonValue.ValueType.STRING) {
            return jo.getString(fieldName);
        } else {
            throw new JobParserException("`" + fieldName + "` should be a STRING");
        }
    }

    static Instant parseSQLiteFormatPartitionField(final JsonObject jo, final String fieldName) {
        try {
            return Utils.parseSqliteFormat(parseStringField(jo, fieldName));
        } catch (final DateTimeParseException e) {
            throw new JobParserException("`" + fieldName + "` field should follow the " + Utils.sqLiteFormatterPattern +" pattern");
        }
    }

    static long parseLongField(final JsonObject jo, final String fieldName) {
        final JsonValue value = parseField(jo, fieldName);
        if(value.getValueType() == JsonValue.ValueType.NUMBER) {
            return jo.getJsonNumber(fieldName).longValue();
        } else {
            throw new JobParserException("`" + fieldName + "` should be a NUMBER");
        }
    }

    static long parsePositiveLongField(final JsonObject jo, final String fieldName) {
        final long value = parseLongField(jo, fieldName);
        if(value < 0) {
            throw new JobParserException("`" + fieldName + "` should be >= 0");
        }
        return value;
    }

    static JsonArray parseJsonArray(final JsonObject jo, final String fieldName) {
        final JsonValue value = parseField(jo, fieldName);
        if(value.getValueType() == JsonValue.ValueType.ARRAY) {
            return value.asJsonArray();
        } else {
            throw new JobParserException("`" + fieldName + "` should be an ARRAY");
        }
    }

    static Dependency[] parseDependencyArray(final JsonObject jo, final String fieldName) {
        final JsonArray array = parseJsonArray(jo, fieldName);
        final Dependency[] ret = new Dependency[array.size()];
        for(int i = 0; i < ret.length; i++) {
            ret[i] = parseDependency(array, i);
        }
        return ret;
    }

    private static Dependency parseDependency(final JsonArray array, final int index) {
        final JsonValue value = array.get(index);
        if(value.getValueType() != JsonValue.ValueType.OBJECT) {
            throw new JobParserException("`dependencies` should by an ARRAY of JsonObject");
        }
        final JsonObject jo = value.asJsonObject();
        return new Dependency(
			      parseStringField(jo, "name"),
                parseDurationField(jo, "shift", false)
        );
    }

    private static String parseStringFromArray(final JsonArray array,
                                               final int index,
                                               final String sourceFieldName) {
        final JsonValue value = array.get(index);
        if(value.getValueType() != JsonValue.ValueType.STRING) {
            throw new JobParserException("`" + sourceFieldName + "` should by an ARRAY of STRING");
        }
        return array.getString(index);
    }

    static String[] parseNonEmptyStringArray(final JsonObject jo, final String fieldName) {
        final String[] arr = parseStringArray(jo, fieldName);
        if(arr.length == 0) {
            throw new JobParserException("`" + fieldName + "` should not be an empty array");
        } else {
            return arr;
        }
    }

    static String[] parseStringArray(final JsonObject jo, final String fieldName) {
        final JsonValue value = parseField(jo, fieldName);
        if(value.getValueType() != JsonValue.ValueType.ARRAY) {
            throw new JobParserException("`" + fieldName + "` should by an ARRAY of STRING");
        }
        final JsonArray array = value.asJsonArray();
        final String[] ret = new String[array.size()];
        for(int i = 0; i < ret.length; i++) {
            ret[i] = parseStringFromArray(array, i, fieldName);
        }
        return ret;
    }

    static Job.FailureBehavior parseFailureBehavior(final JsonObject jo, final String fieldName) {
        final String behaviorString = parseStringField(jo, fieldName).toUpperCase();
        if(Arrays.stream(Job.FailureBehavior.values()).map(Enum::name).noneMatch(value -> value.equals(behaviorString))) {
            throw new JobParserException("`" + fieldName + "` should one of the values: " + Arrays.toString(Job.FailureBehavior.values()));
        }
        return Job.FailureBehavior.valueOf(behaviorString);
    }

    private static long extractValue(final String durationString) {
        final String[] arr = durationString.split(" +");
        return Long.parseLong(arr[0]);
    }

    static Duration parseDurationField(final JsonObject jo,
                                       final String fieldName,
                                       final boolean mustBePositive) {
        final String durationString = parseStringField(jo, fieldName).toLowerCase();

        final String signRegex;
        if(mustBePositive) {
            signRegex = "\\+?";
        } else {
            signRegex = "[\\\\+|-]?";
        }
        final String valueRegex = signRegex + "[0-9]+";

        if(durationString.matches(valueRegex + " millis")) {
            return new Duration(extractValue(durationString), ChronoUnit.MILLIS);
        }
        if(durationString.matches(valueRegex + " seconds")) {
            return new Duration(extractValue(durationString), ChronoUnit.SECONDS);
        }
        if(durationString.matches(valueRegex + " minutes")) {
            return new Duration(extractValue(durationString), ChronoUnit.MINUTES);
        }
        if(durationString.matches(valueRegex + " hours")) {
            return new Duration(extractValue(durationString), ChronoUnit.HOURS);
        }
        if(durationString.matches(valueRegex + " days")) {
            return new Duration(extractValue(durationString), ChronoUnit.DAYS);
        }
        if(durationString.matches(valueRegex + " months")) {
            return new Duration(extractValue(durationString), ChronoUnit.MONTHS);
        }
        if(durationString.matches(valueRegex + " years")) {
            return new Duration(extractValue(durationString), ChronoUnit.YEARS);
        }

        throw new JobParserException("`" + fieldName + "` lowercased should match the regex: `" + valueRegex
                + " (millis|seconds|minutes|hours|days|months|years)`");
    }

}
