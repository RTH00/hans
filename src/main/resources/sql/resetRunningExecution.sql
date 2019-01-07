UPDATE executions SET
  status = "INITIALISED",
  start_time = null
WHERE status = "RUNNING"