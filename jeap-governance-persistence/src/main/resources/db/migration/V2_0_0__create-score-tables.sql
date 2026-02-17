-- system_score
CREATE SEQUENCE system_score_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE system_score
(
    id         BIGINT PRIMARY KEY DEFAULT nextval('system_score_id_seq'),
    score      INTEGER                  NOT NULL,
    system_id  BIGINT                   NOT NULL REFERENCES system (id),
    day        DATE                     NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (system_id, day)
);

ALTER SEQUENCE system_score_id_seq OWNED BY system_score.id;

CREATE INDEX system_score_system_id ON system_score (system_id);
CREATE INDEX system_score_day ON system_score (day);

-- component_score
CREATE SEQUENCE component_score_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE component_score
(
    id                  BIGINT PRIMARY KEY DEFAULT nextval('component_score_id_seq'),
    score               INTEGER                  NOT NULL,
    system_component_id BIGINT                   NOT NULL REFERENCES system_component (id),
    day                 DATE                     NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (system_component_id, day)
);

ALTER SEQUENCE component_score_id_seq OWNED BY component_score.id;

CREATE INDEX component_score_system_component_id ON component_score (system_component_id);
CREATE INDEX component_score_day ON component_score (day);

-- rule_conformance_rate
CREATE SEQUENCE rule_conformance_rate_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE rule_conformance_rate
(
    id               BIGINT PRIMARY KEY DEFAULT nextval('rule_conformance_rate_id_seq'),
    rule_id          VARCHAR                  NOT NULL,
    conformance_rate INTEGER                  NOT NULL,
    day              DATE                     NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (rule_id, day)
);

ALTER SEQUENCE rule_conformance_rate_id_seq OWNED BY rule_conformance_rate.id;

CREATE INDEX rule_conformance_rate_rule_id ON rule_conformance_rate (rule_id);
CREATE INDEX rule_conformance_rate_day ON rule_conformance_rate (day);

-- rule_state
CREATE SEQUENCE rule_state_rate_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE rule_state
(
    id                  BIGINT PRIMARY KEY DEFAULT nextval('rule_state_rate_id_seq'),
    rule_id             VARCHAR                  NOT NULL,
    system_component_id BIGINT                   NOT NULL REFERENCES system_component (id),
    state               VARCHAR                  NOT NULL,
    state_comment       VARCHAR,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (rule_id, system_component_id)
);

ALTER SEQUENCE rule_state_rate_id_seq OWNED BY rule_state.id;

CREATE INDEX rule_state_rule_id ON rule_state (rule_id);
CREATE INDEX rule_state_system_component_id ON rule_state (system_component_id);
CREATE INDEX rule_state_modified_at ON rule_state (modified_at);
