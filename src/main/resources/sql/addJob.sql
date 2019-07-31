INSERT INTO jobs (
  job_name,
  start_partition,
  end_partition,
  increment,
  max_parallelism,
  failure_behavior,
  retry_delay,
  retention,
  stdout_path,
  stderr_path,
  is_activated
)
VALUES (?,?,?,?,?,?,?,?,?,?,?)