-- system
CREATE SEQUENCE system_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE system
(
    id         BIGINT PRIMARY KEY DEFAULT nextval('system_id_seq'),
    name       VARCHAR                  NOT NULL UNIQUE,
    state      VARCHAR                  NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER SEQUENCE system_id_seq OWNED BY system.id;

CREATE INDEX system_name ON system (name);

CREATE TABLE system_aliases
(
    system_id BIGINT  NOT NULL REFERENCES system (id),
    aliases   VARCHAR NOT NULL,
    CONSTRAINT pk_system_aliases PRIMARY KEY (system_id, aliases)
);

CREATE INDEX system_aliases_system_id ON system_aliases (system_id);

-- system_component
CREATE SEQUENCE system_component_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE system_component
(
    id         BIGINT PRIMARY KEY DEFAULT nextval('system_component_id_seq'),
    name       VARCHAR                  NOT NULL UNIQUE,
    system_id  BIGINT                   NOT NULL REFERENCES system (id),
    state      VARCHAR                  NOT NULL,
    type       VARCHAR                  NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER SEQUENCE system_component_id_seq OWNED BY system_component.id;

CREATE INDEX system_component_name ON system_component (name);

-- shedlock
CREATE TABLE shedlock
(
    name       VARCHAR(64),
    lock_until TIMESTAMP(3) NULL,
    locked_at  TIMESTAMP(3) NULL,
    locked_by  VARCHAR(255),
    PRIMARY KEY (name)
);

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
