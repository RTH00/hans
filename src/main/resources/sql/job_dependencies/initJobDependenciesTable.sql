CREATE TABLE IF NOT EXISTS job_dependencies (
 job_name TEXT NOT NULL,
 dependency_job_name TEXT NOT NULL,
 shift_partition TEXT NOT NULL
)