-- API Documentation Versions

CREATE SEQUENCE ar_api_doc_version_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE ar_api_doc_version
(
    id                   BIGINT                   PRIMARY KEY DEFAULT nextval('ar_api_doc_version_id_seq'),
    version              VARCHAR                  NOT NULL,
    system_component_id  BIGINT                   NOT NULL REFERENCES system_component (id),
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ar_api_doc_version_system_component_id ON ar_api_doc_version (system_component_id);

ALTER SEQUENCE ar_api_doc_version_id_seq OWNED BY ar_api_doc_version.id;

-- Database Schema Versions
CREATE SEQUENCE ar_database_schema_version_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE ar_database_schema_version
(
    id                   BIGINT                   PRIMARY KEY DEFAULT nextval('ar_database_schema_version_id_seq'),
    version              VARCHAR                  NOT NULL,
    system_component_id  BIGINT                   NOT NULL REFERENCES system_component (id),
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER SEQUENCE ar_database_schema_version_id_seq OWNED BY ar_database_schema_version.id;

CREATE INDEX ar_database_schema_version_system_component_id ON ar_database_schema_version (system_component_id);

-- Reaction Graphs Last Modified Tracking
CREATE SEQUENCE ar_reaction_graph_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE ar_reaction_graph
(
    id                   BIGINT                   PRIMARY KEY DEFAULT nextval('ar_reaction_graph_id_seq'),
    last_modified_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    system_component_id  BIGINT                   NOT NULL REFERENCES system_component (id),
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER SEQUENCE ar_reaction_graph_id_seq OWNED BY ar_reaction_graph.id;

CREATE INDEX ar_reaction_graph_system_component_id ON ar_reaction_graph (system_component_id);

-- REST API Relations (Consumer-Provider relationships)
CREATE SEQUENCE ar_rest_api_relation_without_pact_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE ar_rest_api_relation_without_pact
(
    id                            BIGINT                   PRIMARY KEY DEFAULT nextval('ar_rest_api_relation_without_pact_id_seq'),
    method                        VARCHAR                  NOT NULL,
    path                          VARCHAR                  NOT NULL,
    provider_system_component_id  BIGINT                   NOT NULL REFERENCES system_component (id),
    consumer_system_component_id  BIGINT                   NOT NULL REFERENCES system_component (id),
    created_at                    TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER SEQUENCE ar_rest_api_relation_without_pact_id_seq OWNED BY ar_rest_api_relation_without_pact.id;

CREATE INDEX ar_rest_api_relation_without_pact_provider_system_component_id ON ar_rest_api_relation_without_pact (provider_system_component_id);
CREATE INDEX ar_rest_api_relation_without_pact_consumer_system_component_id ON ar_rest_api_relation_without_pact (consumer_system_component_id);
