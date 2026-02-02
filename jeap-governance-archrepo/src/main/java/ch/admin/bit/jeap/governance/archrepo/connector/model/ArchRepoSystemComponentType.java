package ch.admin.bit.jeap.governance.archrepo.connector.model;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum ArchRepoSystemComponentType {
    BACKEND_SERVICE,
    FRONTEND,
    MOBILE_APP,
    SELF_CONTAINED_SYSTEM,
    @JsonEnumDefaultValue
    UNKNOWN
}
