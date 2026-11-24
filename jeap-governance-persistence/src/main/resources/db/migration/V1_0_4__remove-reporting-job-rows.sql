-- The reporting job has been removed: drop its last run and its lock.
DELETE FROM scheduler_run WHERE job_name = 'reporting';
DELETE FROM shedlock WHERE name = 'reporting';
