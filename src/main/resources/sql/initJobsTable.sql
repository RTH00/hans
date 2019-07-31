CREATE TABLE IF NOT EXISTS jobs (
 job_name TEXT PRIMARY KEY UNIQUE NOT NULL,
 start_partition DATE NOT NULL,
 end_partition DATE NOT NULL,
 increment TEXT NOT NULL,
 max_parallelism INTEGER NOT NULL,
 failure_behavior TEXT NOT NULL,
 retry_delay TEXT NOT NULL,
 retention TEXT NOT NULL,
 stdout_path TEXT NOT NULL,
 stderr_path TEXT NOT NULL,
 is_activated BOOLEAN NOT NULL
)