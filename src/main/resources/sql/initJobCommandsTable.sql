CREATE TABLE IF NOT EXISTS job_commands (
 job_name TEXT NOT NULL,
 ordering INTEGER NOT NULL,
 command TEXT NOT NULL,
 CONSTRAINT unique_job_name_ordering UNIQUE(job_name, ordering)
)