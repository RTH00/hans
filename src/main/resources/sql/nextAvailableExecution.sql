SELECT job_name,
       partition
FROM (
SELECT job_name,
       partition,
       min(case when ready = "SUCCESS" then 1 else 0 end) as ready
FROM (
  SELECT src.job_name,
         src.partition,
         job_dependencies.dependency_job_name,
         case when job_dependencies.dependency_job_name IS NULL then "SUCCESS" else executions.status end AS ready
  FROM (
    SELECT executions.job_name AS job_name, executions.partition AS partition
    FROM executions
    INNER JOIN jobs ON jobs.job_name = executions.job_name
    LEFT JOIN job_running_instances ON jobs.job_name = job_running_instances.job_name
    WHERE jobs.max_parallelism > coalesce(job_running_instances.counter, 0)
    AND (executions.status = "INITIALISED" OR status = "FAILURE")
    AND executions.next_schedule_time <= ?
  ) AS src
  LEFT JOIN job_dependencies ON src.job_name = job_dependencies.job_name
  LEFT JOIN executions ON job_dependencies.dependency_job_name = executions.job_name
        AND datetime(src.partition, job_dependencies.shift_partition) = executions.partition
) dep_status
GROUP BY job_name, partition
) as list
WHERE ready = 1
LIMIT 1000