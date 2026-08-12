CREATE SEQUENCE mc_version_status_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE mc_version_status
(
    id             BIGINT PRIMARY KEY DEFAULT nextval('mc_version_status_id_seq'),
    app_name       VARCHAR NOT NULL,
    app_version    VARCHAR NOT NULL,
    message_type   VARCHAR NOT NULL,
    used_version   VARCHAR NOT NULL,
    latest_version VARCHAR NOT NULL,
    topic          VARCHAR NOT NULL,
    role           VARCHAR NOT NULL,
    up_to_date     BOOLEAN NOT NULL
);

CREATE INDEX mc_version_status_app_outdated ON mc_version_status (app_name, up_to_date);
ALTER SEQUENCE mc_version_status_id_seq OWNED BY mc_version_status.id;
