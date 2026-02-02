-- API Documentation Versions
CREATE TABLE ar_api_doc_version
(
    id                   UUID                     PRIMARY KEY,
    version              VARCHAR                  NOT NULL,
    system_component_id  UUID                     NOT NULL REFERENCES system_component (id),
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ar_api_doc_version_system_component_id ON ar_api_doc_version (system_component_id);

-- Database Schema Versions
CREATE TABLE ar_database_schema_version
(
    id                   UUID                     PRIMARY KEY,
    version              VARCHAR                  NOT NULL,
    system_component_id  UUID                     NOT NULL REFERENCES system_component (id),
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ar_database_schema_version_system_component_id ON ar_database_schema_version (system_component_id);

-- Reaction Graphs Last Modified Tracking
CREATE TABLE ar_reaction_graph
(
    id                   UUID                     PRIMARY KEY,
    last_modified_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    system_component_id  UUID                     NOT NULL REFERENCES system_component (id),
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ar_reaction_graph_system_component_id ON ar_reaction_graph (system_component_id);

-- REST API Relations (Consumer-Provider relationships)
CREATE TABLE ar_rest_api_relation_without_pact
(
    id                            UUID                     PRIMARY KEY,
    method                        VARCHAR                  NOT NULL,
    path                          VARCHAR                  NOT NULL,
    provider_system_component_id  UUID                     NOT NULL REFERENCES system_component (id),
    consumer_system_component_id  UUID                     NOT NULL REFERENCES system_component (id),
    created_at                    TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ar_rest_api_relation_without_pact_provider_system_component_id ON ar_rest_api_relation_without_pact (provider_system_component_id);
CREATE INDEX ar_rest_api_relation_without_pact_consumer_system_component_id ON ar_rest_api_relation_without_pact (consumer_system_component_id);
