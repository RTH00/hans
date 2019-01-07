UPDATE executions SET start_time = ?, end_time = null, status = "RUNNING"
WHERE job_name = ? AND partition = ?