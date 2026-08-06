package ch.admin.bit.jeap.governance.domain;

public enum ComponentType {
    BACKEND_SERVICE,
    FRONTEND,
    MOBILE_APP,
    SELF_CONTAINED_SYSTEM,
    GATEWAY,
    UNKNOWN;

    /**
     * Returns true if components of this type should be ignored for governance rule evaluation,
     * scoring, and reporting.
     */
    public boolean isIgnoredForGovernance() {
        return this == GATEWAY;
    }

    public static boolean isIgnoredForGovernance(ComponentType componentType) {
        return componentType != null && componentType.isIgnoredForGovernance();
    }
}
