-- Reaction Observer schema
-- Component last_observation_date table

CREATE SEQUENCE ro_component_last_observation_date_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE ro_component_last_observation_date
(
    id                    BIGINT PRIMARY KEY DEFAULT nextval('ro_component_last_observation_date_id_seq'),
    last_observation_date DATE                     NOT NULL,
    system_component_id   BIGINT                   NOT NULL REFERENCES system_component (id),
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ro_component_last_observation_date_system_component_id ON ro_component_last_observation_date (system_component_id);

ALTER SEQUENCE ro_component_last_observation_date_id_seq OWNED BY ro_component_last_observation_date.id;

