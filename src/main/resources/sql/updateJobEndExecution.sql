UPDATE executions SET end_time = ?, next_schedule_time = ?, status = ?
WHERE job_name = ? AND partition = ?