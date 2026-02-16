-- secscan state
CREATE SEQUENCE secscan_state_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE secscan_state
(
    id                     BIGINT                   PRIMARY KEY DEFAULT nextval('secscan_state_seq'),
    system_component_id    BIGINT                   NOT NULL REFERENCES system_component (id),
    scan_message           VARCHAR,
    scan_timestamp         TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER SEQUENCE secscan_state_seq OWNED BY secscan_state.id;

CREATE UNIQUE INDEX secscan_state_system_component_id ON secscan_state (system_component_id);

-- secscan flagged endpoint
CREATE SEQUENCE secscan_flagged_endpoint_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE secscan_flagged_endpoint
(
    id                     BIGINT                   PRIMARY KEY DEFAULT nextval('secscan_flagged_endpoint_seq'),
    system_component_id    BIGINT                   NOT NULL REFERENCES system_component (id),
    path                   VARCHAR                  NOT NULL,
    method                 VARCHAR                  NOT NULL,
    scan_message           VARCHAR,
    scan_timestamp         TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER SEQUENCE secscan_flagged_endpoint_seq OWNED BY secscan_flagged_endpoint.id;

CREATE INDEX secscan_flagged_endpoint_system_component_id ON secscan_flagged_endpoint (system_component_id);
