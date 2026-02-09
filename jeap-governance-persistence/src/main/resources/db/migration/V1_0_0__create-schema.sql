-- system
CREATE SEQUENCE system_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE system
(
    id         BIGINT                       PRIMARY KEY DEFAULT nextval('system_id_seq'),
    name       VARCHAR                      NOT NULL UNIQUE,
    state      VARCHAR                      NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE     NOT NULL
);

ALTER SEQUENCE system_id_seq OWNED BY system.id;

CREATE INDEX system_name ON system (name);

CREATE TABLE system_aliases
(
    system_id  BIGINT    NOT NULL REFERENCES system (id),
    aliases    VARCHAR   NOT NULL,
    CONSTRAINT pk_system_aliases PRIMARY KEY (system_id, aliases)
);

CREATE INDEX system_aliases_system_id ON system_aliases (system_id);

-- system_component
CREATE SEQUENCE system_component_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE system_component
(
    id         BIGINT                       PRIMARY KEY DEFAULT nextval('system_component_id_seq'),
    name       VARCHAR                      NOT NULL UNIQUE,
    system_id  BIGINT                       NOT NULL REFERENCES system (id),
    state      VARCHAR                      NOT NULL,
    type       VARCHAR                      NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE     NOT NULL
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
