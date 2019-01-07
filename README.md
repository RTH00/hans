# H.A.N.S.

## The Handy And Nimble Scheduler.

### In a few words

H.A.N.S. was designed to provide basic scheduler features, to be easy to deploy and to update the job graph without job interruptions.

### Features

* Basics scheduler features.
  * Batch/Partition pattern.
  * Dependency resolution.
  * Failure behavior.
  * Parallelism management.
* Hot reloading of the configuration, without interrupting running jobs.
* JSON configuration.
* Running on the JVM

## Configuration file

Jobs and scheduler parameters are defined in a single configuration file.

### Hot reloading

When the configuration is successfully parsed, the file is renamed by adding "_IMPORTED" as a suffix of the file name. At the next iteration (defined by the `new_configuration_polling_time` field) the scheduler will be looking at a new file in the original location and load it if available.

### Configuration example

Let's have a look at this configuration file:

```
{
  "max_parallelism": 10,
  "scheduler_polling_time": "1000 millis",
  "new_configuration_polling_time": "2 minutes",
  "jobs": [
    {
      "name": "job1",
      "start_partition": "2000-01-01 00:00:00",
      "end_partition": "2000-01-03 00:00:00",
      "increment": "1 hours",
      "command": [
        "/bin/echo",
    	"[[partition]]"
      ],
      "dependencies": [],
      "max_parallelism": 6,
      "on_failure": "retry",
      "retry_delay": "15 minutes",
      "retention": "30 days",
      "stdout_path": "stdout[[partition]].txt",
      "stderr_path": "stderr[[partition]].txt"
    },
    {
      "name": "job2",
      "start_partition": "2000-01-01 00:00:00",
      "end_partition": "2000-01-03 00:00:00",
      "increment": "1 hours",
      "command": [
        "/bin/bash",
	"-c",
	"sleep 10; false"
      ],
      "dependencies": [
        {
          "name": "job1",
          "shift": "0 seconds"
        },
        {
          "name": "job1",
          "shift": "-1 hours"
        }
      ],
      "max_parallelism": 6,
      "on_failure": "mark_success",
      "retry_delay": "0 seconds",
      "retention": "100 days",
      "stdout_path": "/home/something/something/log.txt",
      "stderr_path": "/home/something/something/log.txt"
    }
  ]
}
```

This configuration defines two jobs:

The first one will simply echo 48 times the current partition in 48 files (there are 48 hourly partition between `"start_partition": "2000-01-01 00:00:00"` and `"end_partition": "2000-01-03 00:00:00"`).
The second will execute the bash command `sleep 10; false` which always failed (return an error code != 0) but because the `on_failure` policy is set to `mark_success`, the job will not be retried. Another particularity is that this job has a dependency on the same and previous partition of the first job, hence it will never be triggered for the partition `2000-01-01 00:00:00` because the job1 was never executed for the partition `1999-12-31 00:00:00`).

### Fields description

* max_parallelism: Maximum number of concurrent processes started by the scheduler.
* scheduler_polling_time: Time between two 'scheduling iteration', increasing this value will lower the footprint of the scheduler but increasing the job scheduling latency. Recommended default value: "1 seconds".
* new_configuration_polling_time: Time between two parse of the configuration file (if available), increasing this value will lower the footprint of the scheduler but increasing the latency. Recommended default value: "2 minutes".
* jobs: List of the jobs executed by the scheduler.
  * start_partition: first partition date all partition dates should have the following format: "yyyy-MM-dd HH:mm:ss".
  * end_partition: date of the last partition (excluded), to run this task continuously set a date in a distant future.
  * increment: Duration used to generate new partitions from the starting date.
  * command: Array of Strings used to start the task. Templating applies to this field (see the `Templating` section).
  * dependencies: list of the jobs which the job depends on. The job can depend on itself (take care to avoid deadlocks).
    * name: Name of the job on which the current job depends.
    * shift: Duration applied to the current partition to compute the dependency matching partition.
  * max_parallelism: Maximum number of concurrent processes executing this job. This parameter can be set to 0 to 'pause' a job. There is never more than one process per job and partition.
  * on_failure: Behavior in case of failure of the job either `retry` (task is relaunched until it works) or `mark_success` (task is internally registered as a success and won't block dependent jobs)
  * retry_delay: Duration before retrying a failing task with "retry" as a on_failure behavior.
  * retention: Retention applied in the internal database, this only cleans the internal database and doesn't affect the logs.
  * stdout_path: Templating applies to this field (see the `Templating` section).
  * stderr_path: Same as `stdout_path` for the standard error output. Failure to start the job process itself is written in this file.

All fields are mandatory but the `jobs` and `dependencies` lists can be empty.

## Execution model

Each job is triggered for each partition generated from the `start_partition` and `increment` fields. A partition execution is started after the start of the next partition. For example, the partition `2000-01-01 00:00:00` of an hourly job will be triggered after `2000-01-01 01:00:00`.

## Templating

In the `command`, `stdout_path` and `stderr_path` fields, the "[[partition]]" is replaced with the partition corresponding to this execution with the "yyyy-MM-dd HH:mm:ss" format. For example is you want to split the job logs by partition, you can set `"stdout_path": "logs/my_job_name/[[partition]]/logs.txt"`

## Building H.A.N.S.

The test suite is relying on Linux-style paths and commands/binaries, this project should compile on Linux & Mac but won't on other systems.

Simply git clone the project and execute `mvn clean package` in the root directory of the project. The jar with dependencies should be available at `target/hans-[VERSION]-jar-with-dependencies.jar`.

## Starting H.A.N.S.

### Prepare a valid configuration

H.A.N.S. needs a first valid initial configuration. You can use the following one which does not contain any job as a first test:

```
{
  "max_parallelism": 10,
  "scheduler_polling_time": "2000 millis",
  "new_configuration_polling_time": "2 minutes",
  "jobs": []
}
```

### Start the .jar

After building the jar with dependencies (see section `Building H.A.N.S.`). Two options are available:

* runScheduler: Start the scheduler and start executing jobs.
`java -jar target/hans-1.0.0-jar-with-dependencies.jar runScheduler [Configuration file path] [Database file path]`
* parseConfigurationTest: This option is used to test the parsing of a configuration file.
`java -jar target/hans-1.0.0-jar-with-dependencies.jar parseConfigurationTest [Configuration file path]`

H.A.N.S. has a minimal memory footprint, the following jvm options can be used: `-XX:+UseSerialGC -Xmx100m -Xms100m`.

## Notes

* Job priority is not supported.
* Restarting a job marked as success (backfilling) is not supported.
* Every date (including logging) is using UTC.
* SQLite is used a backend database, it is not recommended to manipulate it directly.
* Dependency shift is discrete and cannot be defined as an interval.