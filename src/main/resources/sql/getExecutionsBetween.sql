SELECT job_name,
       partition,
       status,
       start_time,
       end_time,
       next_schedule_time
FROM executions
WHERE job_name = ?
ORDER BY partition DESC
LIMIT ?
OFFSET ?