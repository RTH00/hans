INSERT INTO executions (job_name, partition, status, start_time, end_time, next_schedule_time)
values (?,?,"INITIALISED",null,null,?)
ON CONFLICT (job_name, partition) DO NOTHING