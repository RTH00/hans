CREATE TABLE IF NOT EXISTS executions (
  job_name TEXT NOT NULL,
  partition DATE NOT NULL,
  status TEXT NOT NULL,
  start_time DATE,
  end_time DATE,
  next_schedule_time DATE,
  CONSTRAINT uniq_job_name_partition UNIQUE (job_name, partition)
)