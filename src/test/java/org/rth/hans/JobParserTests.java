package org.rth.hans;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import java.io.StringReader;
import java.time.temporal.ChronoUnit;

public class JobParserTests {

    private static JsonObject parseJsonObject(final String input) {
        return Json.createReader(new StringReader(input)).readObject();
    }

    @Test
    void parseStringFieldTest1() throws Exception {
        final JsonObject jo = parseJsonObject("{ \"name\": \"I am a name\"}");
        Assertions.assertEquals("I am a name", JobParser.parseStringField(jo, "name"));
    }

    @Test
    void parseStringFieldTest2() throws Exception {
        final JsonObject jo = parseJsonObject("{ \"name\": 999}");
        try {
            JobParser.parseStringField(jo, "name");
            Assertions.fail("Should raise a JobParserException");
        } catch (final JobParser.JobParserException e) {
            Assertions.assertEquals("`name` should be a STRING", e.getMessage());
        }
    }

    @Test
    void parseStringFieldTest3() throws Exception {
        final JsonObject jo = parseJsonObject("{}");
        try {
            JobParser.parseStringField(jo, "name");
            Assertions.fail("Should raise a JobParserException");
        } catch (final JobParser.JobParserException e) {
            Assertions.assertEquals("Can't find `name` field", e.getMessage());
        }
    }

    @Test
    void parseSQLiteFormatPartitionTest1() throws Exception {
        final JsonObject jo = parseJsonObject("{ \"date\": \"2000-01-01 01:01:01\"}");
        Assertions.assertEquals("2000-01-01T01:01:01Z", JobParser.parseSQLiteFormatPartitionField(jo, "date").toString());
    }

    @Test
    void parseSQLiteFormatPartitionTest2() throws Exception {
        final JsonObject jo = parseJsonObject("{ \"date\": \"INVALID\"}");
        try {
            JobParser.parseSQLiteFormatPartitionField(jo, "date");
            Assertions.fail("Should throw JobParserException");
        } catch (final JobParser.JobParserException e) {
            Assertions.assertEquals("`date` field should follow the yyyy-MM-dd HH:mm:ss pattern", e.getMessage());
        }
    }

    @Test
    void parseLongFieldTest1() throws Exception {
        final JsonObject jo = parseJsonObject("{ \"number\": \"TEXT_INVALID\"}");
        try {
            JobParser.parseLongField(jo, "number");
        } catch(final JobParser.JobParserException e) {
            Assertions.assertEquals("`number` should be a NUMBER", e.getMessage());
        }
    }

    @Test
    void parseLongFieldTest2() throws Exception {
        final JsonObject jo = parseJsonObject("{ \"number\": 12345}");
        Assertions.assertEquals(12345L, JobParser.parseLongField(jo, "number"));
    }

    @Test
    void parseLongFieldTest3() throws Exception {
        final JsonObject jo = parseJsonObject("{ \"number\": 999999999999999999999999999999999999999999999999}");
        Assertions.assertEquals(9169610316303040511L, JobParser.parseLongField(jo, "number"));
    }

    @Test
    void parseLongFieldTest4() throws Exception {
        final JsonObject jo = parseJsonObject("{ \"number\": -999999999999999999999999999999999999999999999999}");
        Assertions.assertEquals(-9169610316303040511L, JobParser.parseLongField(jo, "number"));
    }

    @Test
    void parsePositiveLongFieldTest1() throws Exception {
        final JsonObject jo = parseJsonObject("{ \"number\": 12345}");
        Assertions.assertEquals(12345L, JobParser.parsePositiveLongField(jo, "number"));
    }

    @Test
    void parsePositiveLongFieldTest2() throws Exception {
        final JsonObject jo = parseJsonObject("{ \"number\": -1 }");
        try {
            JobParser.parsePositiveLongField(jo, "number");
        } catch(final JobParser.JobParserException e) {
            Assertions.assertEquals("`number` should be >= 0", e.getMessage());
        }
    }

    @Test
    void parseJsonArrayTest1() throws Exception {
        final JsonObject jo = parseJsonObject("{ \"arr\": [\"\", \"\"]}");
        Assertions.assertEquals(2, JobParser.parseJsonArray(jo, "arr").size());
    }

    @Test
    void parseJsonArrayTest2() throws Exception {
        final JsonObject jo = parseJsonObject("{ \"arr\": 123}");
        try {
            JobParser.parseJsonArray(jo, "arr");
        } catch(final JobParser.JobParserException e) {
            Assertions.assertEquals("`arr` should be an ARRAY", e.getMessage());
        }
    }

    @Test
    void parseDependencyArrayTest1() throws Exception {
        final JsonObject jo = parseJsonObject("{ \"dependencies\": [\"invalid string\"]}");
        try {
            JobParser.parseDependencyArray(jo, "dependencies");
        } catch(final JobParser.JobParserException e) {
            Assertions.assertEquals("`dependencies` should by an ARRAY of JsonObject", e.getMessage());
        }
    }

    @Test
    void parseDependencyArrayTest2() throws Exception {
        final JsonObject jo = parseJsonObject("{ \"dependencies\": [{\"name\": \"toto\", \"shift\": \"123\"}]}");
        try {
            JobParser.parseDependencyArray(jo, "dependencies");
        } catch(final JobParser.JobParserException e) {
            Assertions.assertEquals("`shift` lowercased should match the regex: `[\\\\+|-]?[0-9]+ (millis|seconds|minutes|hours|days|months|years)`", e.getMessage());
        }
    }

    @Test
    void parseDependencyArrayTest3() throws Exception {
        final JsonObject jo = parseJsonObject("{ \"dependencies\": [{\"name\": \"toto\", \"shift\": \"123 seconds\"}]}");
        final Dependency[] dependencies = JobParser.parseDependencyArray(jo, "dependencies");
        Assertions.assertEquals(1, dependencies.length);
        final Dependency dependency = dependencies[0];
        Assertions.assertEquals("toto", dependency.getJobName());
        Assertions.assertEquals(123, dependency.getShift().getValue());
        Assertions.assertEquals(ChronoUnit.SECONDS, dependency.getShift().getUnit());
    }

    @Test
    void parseStringArrayTest1() throws Exception {
        final JsonObject jo = parseJsonObject("{ \"command\": 123}");
        try {
            JobParser.parseStringArray(jo, "command");
        } catch(final JobParser.JobParserException e) {
            Assertions.assertEquals("`command` should by an ARRAY of STRING", e.getMessage());
        }
    }

    @Test
    void parseStringArrayTest2() throws Exception {
        final JsonObject jo = parseJsonObject("{ \"command\": [123]}");
        try {
            JobParser.parseStringArray(jo, "command");
        } catch(final JobParser.JobParserException e) {
            Assertions.assertEquals("`command` should by an ARRAY of STRING", e.getMessage());
        }
    }

    @Test
    void parseStringArrayTest3() throws Exception {
        final JsonObject jo = parseJsonObject("{ \"command\": [\"/bin/echo\", \"toto\"]}");
        final String[] command = JobParser.parseStringArray(jo, "command");
        Assertions.assertArrayEquals(new String[]{"/bin/echo", "toto"}, command);
    }

    @Test
    void parseNonEmptyStringArrayTest() throws Exception {
        final JsonObject jo = parseJsonObject("{ \"command\": []}");
        try {
            final String[] command = JobParser.parseNonEmptyStringArray(jo, "command");
            Assertions.fail("Should throw a JobParserException");
        } catch(final JobParser.JobParserException e) {
            Assertions.assertEquals("`command` should not be an empty array", e.getMessage());
        }
    }

    @Test
    void parseFailureBehaviorTest1() throws Exception {
        final JsonObject jo = parseJsonObject("{ \"on_failure\": 123}");
        try {
            JobParser.parseFailureBehavior(jo, "on_failure");
        } catch(final JobParser.JobParserException e) {
            Assertions.assertEquals("`on_failure` should be a STRING", e.getMessage());
        }
    }

    @Test
    void parseFailureBehaviorTest2() throws Exception {
        final JsonObject jo = parseJsonObject("{ \"on_failure\": \"INVALID_STRING\"}");
        try {
            JobParser.parseFailureBehavior(jo, "on_failure");
        } catch(final JobParser.JobParserException e) {
            Assertions.assertEquals("`on_failure` should one of the values: [RETRY, MARK_SUCCESS]", e.getMessage());
        }
    }

    @Test
    void parseFailureBehaviorTest3() throws Exception {
        final JsonObject jo = parseJsonObject("{ \"on_failure\": \"RETRY\"}");
        Assertions.assertEquals(Job.FailureBehavior.RETRY, JobParser.parseFailureBehavior(jo, "on_failure"));
    }

    @Test
    void parseFailureBehaviorTest4() throws Exception {
        final JsonObject jo = parseJsonObject("{ \"on_failure\": \"retry\"}");
        Assertions.assertEquals(Job.FailureBehavior.RETRY, JobParser.parseFailureBehavior(jo, "on_failure"));
    }

    @Test
    void parseJobs1Test() throws Exception {
        final String input = " [\"\", \"\" ";
        try {
            JobParser.parseJobConfiguration(input);
        } catch(final JobParser.JobParserException e) {
            Assertions.assertEquals("Job configuration should be a valid Json, parser error: Invalid token=EOF at (line no=1, column no=18, offset=17). Expected tokens are: [CURLYOPEN, SQUAREOPEN, STRING, NUMBER, TRUE, FALSE, NULL, SQUARECLOSE]", e.getMessage());
        }
    }

    @Test
    void parseJobs2Test() throws Exception {
        final String input = "";
        try {
            JobParser.parseJobConfiguration(input);
        } catch(final JobParser.JobParserException e) {
            Assertions.assertEquals("Job configuration should be a valid Json, parser error: Internal Error", e.getMessage());
        }
    }

    @Test
    void parseJobs3Test() throws Exception {
        final String input = "[]";
        try {
            JobParser.parseJobConfiguration(input);
        } catch(final JobParser.JobParserException e) {
            Assertions.assertEquals("Job configuration should be a JsonObject", e.getMessage());
        }
    }

    @Test
    void parseJobs4Test() throws Exception {
        try {
            JobParser.parseJobConfiguration(Utils.readResource("jobs/parseJobs4Graph.json"));
        } catch(final JobParser.JobParserException e) {
            Assertions.assertEquals("A job should be a JsonObject", e.getMessage());
        }
    }

    @Test
    void parseJobs5Test() throws Exception {
        try {
            JobParser.parseJobConfiguration(Utils.readResource("jobs/parseJobs5Graph.json"));
        } catch(final JobParser.JobParserException e) {
            Assertions.assertEquals("`jobs` should be an ARRAY", e.getMessage());
        }
    }

    @Test
    void parseDurationTest1() throws Exception {
        final String[] validJsons = new String[]{
                "{ \"increment\": \"-10 millis\"}",
                "{ \"increment\": \"-10 seconds\"}",
                "{ \"increment\": \"-10 hours\"}",
                "{ \"increment\": \"-10 days\"}",
                "{ \"increment\": \"-10 months\"}",
                "{ \"increment\": \"-10 years\"}",
                "{ \"increment\": \"-10 SECONDS\"}",
                "{ \"increment\": \"-10 HOURS\"}",
                "{ \"increment\": \"-10 DAYS\"}",
                "{ \"increment\": \"-10 MONTHS\"}",
                "{ \"increment\": \"-10 YEARS\"}",
                "{ \"increment\": \"10 seconds\"}",
                "{ \"increment\": \"+10 seconds\"}"
        };
        final Duration[] expectedResults = new Duration[]{
                new Duration(-10, ChronoUnit.MILLIS),
                new Duration(-10, ChronoUnit.SECONDS),
                new Duration(-10, ChronoUnit.HOURS),
                new Duration(-10, ChronoUnit.DAYS),
                new Duration(-10, ChronoUnit.MONTHS),
                new Duration(-10, ChronoUnit.YEARS),
                new Duration(-10, ChronoUnit.SECONDS),
                new Duration(-10, ChronoUnit.HOURS),
                new Duration(-10, ChronoUnit.DAYS),
                new Duration(-10, ChronoUnit.MONTHS),
                new Duration(-10, ChronoUnit.YEARS),
                new Duration(10, ChronoUnit.SECONDS),
                new Duration(10, ChronoUnit.SECONDS)
        };

        Assertions.assertEquals(expectedResults.length, validJsons.length, "results and inputs should have the same length");

        for(int i = 0; i < validJsons.length; i++) {
            final JsonObject jo = parseJsonObject(validJsons[i]);
            final Duration expectedDuration = expectedResults[i];
            final Duration duration = JobParser.parseDurationField(jo, "increment", false);
            Assertions.assertEquals(expectedDuration.getValue(), duration.getValue());
            Assertions.assertEquals(expectedDuration.getUnit(), duration.getUnit());
        }
    }

    @Test
    void parseInvalidPositiveDurationTest() throws Exception {

        final String[] failingDurations = new String[] {
                "{ \"increment\": \"-10 seconds\"}"
        };

        for(final String input: failingDurations) {
            final JsonObject jo = parseJsonObject(input);
            try {
                JobParser.parseDurationField(jo, "increment", true);
                Assertions.fail("Should trigger JobParserException");
            } catch(final JobParser.JobParserException e) {
                Assertions.assertEquals("`increment` lowercased should match the regex: `\\+?[0-9]+ (millis|seconds|minutes|hours|days|months|years)`", e.getMessage());
            }
        }
    }

    @Test
    void parseInvalidDurationTest() throws Exception {

        final String[] failingDurations = new String[] {
                "{ \"increment\": \"-123456 days|seconds\"}",
                "{ \"increment\": \"123456 day\"}",
                "{ \"increment\": \"123456 \"}",
                "{ \"increment\": \"123456\"}",
                "{ \"increment\": \"123AAA456 days\"}"
        };

        for(final String input: failingDurations) {
            final JsonObject jo = parseJsonObject(input);
            try {
                JobParser.parseDurationField(jo, "increment", false);
                Assertions.fail("Should trigger JobParserException");
            } catch(final JobParser.JobParserException e) {
                Assertions.assertEquals("`increment` lowercased should match the regex: `[\\\\+|-]?[0-9]+ (millis|seconds|minutes|hours|days|months|years)`", e.getMessage());
            }
        }
    }

    @Test
    void parseInvalidDurationTypeTest() throws Exception {
        final JsonObject jo = parseJsonObject("{ \"increment\": 3600}");
        try {
            JobParser.parseDurationField(jo, "increment", true);
            Assertions.fail("Should trigger JobParserException");
        } catch(final JobParser.JobParserException e) {
            Assertions.assertEquals("`increment` should be a STRING", e.getMessage());
        }
    }

    @Test
    void sanityCheck1Test() throws Exception {
        try {
            JobParser.parseJobConfiguration(Utils.readResource("jobs/sanityCheck1Graph.json"));
            Assertions.fail("Should raise a JobParserException");
        } catch (final JobParser.JobParserException e) {
            Assertions.assertEquals("Job name: `job2` is already used", e.getMessage());
        }
    }

    @Test
    void sanityCheck2Test() throws Exception {
        try {
            JobParser.parseJobConfiguration(Utils.readResource("jobs/sanityCheck2Graph.json"));
            Assertions.fail("Should raise a JobParserException");
        } catch (final JobParser.JobParserException e) {
            Assertions.assertEquals("Dependency on `blablabla`is invalid, this job name is invalid", e.getMessage());
        }
    }
}
