-- Deployment Log schema
-- Component version table

CREATE SEQUENCE dl_component_version_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE dl_component_version
(
    id                   BIGINT                   PRIMARY KEY DEFAULT nextval('dl_component_version_id_seq'),
    version              VARCHAR                  NOT NULL,
    system_component_id  BIGINT                   NOT NULL REFERENCES system_component (id),
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX dl_component_version_system_component_id ON dl_component_version (system_component_id);

ALTER SEQUENCE dl_component_version_id_seq OWNED BY dl_component_version.id;

