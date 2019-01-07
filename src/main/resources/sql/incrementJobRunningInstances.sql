INSERT INTO job_running_instances (job_name, counter) VALUES (?, 1)
 ON CONFLICT(job_name) DO UPDATE SET counter = counter + 1 WHERE job_name = ?