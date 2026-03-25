-- scheduler_run
CREATE TABLE scheduler_run
(
    job_name    VARCHAR   NOT NULL PRIMARY KEY,
    last_run_at TIMESTAMP NOT NULL
);
