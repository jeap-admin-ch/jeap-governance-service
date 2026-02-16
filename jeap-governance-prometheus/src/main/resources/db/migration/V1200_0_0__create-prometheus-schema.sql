-- prometheus time series
CREATE SEQUENCE prom_time_series_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE prom_time_series
(
    id                     BIGINT                   PRIMARY KEY DEFAULT nextval('prom_time_series_seq'),
    prometheus_query_type  VARCHAR                  NOT NULL,
    query_timestamp        TIMESTAMP WITH TIME ZONE NOT NULL,
    system_component_name  VARCHAR                  NOT NULL,
    sample                 JSONB                    NOT NULL
);

ALTER SEQUENCE prom_time_series_seq OWNED BY prom_time_series.id;

CREATE INDEX prom_time_series_system_component_name ON prom_time_series (system_component_name);
CREATE INDEX prom_time_series_prometheus_query_type ON prom_time_series (prometheus_query_type);
