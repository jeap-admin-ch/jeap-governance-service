CREATE TABLE system
(
    id         UUID                         PRIMARY KEY,
    name       VARCHAR                      NOT NULL UNIQUE,
    state      VARCHAR                      NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE     NOT NULL
);

CREATE INDEX system_name ON system (name);

CREATE TABLE system_aliases
(
    system_id  UUID    NOT NULL REFERENCES system (id),
    aliases    VARCHAR NOT NULL,
    CONSTRAINT pk_system_aliases PRIMARY KEY (system_id, aliases)
);

CREATE INDEX system_aliases_system_id ON system_aliases (system_id);

CREATE TABLE system_component
(
    id         UUID                         PRIMARY KEY,
    name       VARCHAR                      NOT NULL UNIQUE,
    system_id  UUID                         NOT NULL REFERENCES system (id),
    state      VARCHAR                      NOT NULL,
    type       VARCHAR                      NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE     NOT NULL
);

CREATE INDEX system_component_name ON system_component (name);


CREATE TABLE shedlock
(
    name       VARCHAR(64),
    lock_until TIMESTAMP(3) NULL,
    locked_at  TIMESTAMP(3) NULL,
    locked_by  VARCHAR(255),
    PRIMARY KEY (name)
);
