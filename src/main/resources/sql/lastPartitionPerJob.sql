SELECT jobs.job_name AS job_name,
       jobs.start_partition AS start_partition,
       max(executions.partition) AS last_partition
FROM jobs
LEFT JOIN executions ON jobs.job_name = executions.job_name
GROUP BY jobs.job_name, jobs.start_partition