package ch.admin.bit.jeap.governance.archrepo.connector.model;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum ArchRepoComponentImporter {
    GRAFANA,
    DEPLOYMENT_LOG,
    MESSAGE_TYPE_REGISTRY,
    PACT_BROKER,
    OPEN_API,
    REST_CONTROLLER,
    @JsonEnumDefaultValue
    UNKNOWN
}
