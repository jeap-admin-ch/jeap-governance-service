package ch.admin.bit.jeap.governance.domain;

public enum ComponentType {
    BACKEND_SERVICE,
    FRONTEND,
    MOBILE_APP,
    SELF_CONTAINED_SYSTEM,
    UNKNOWN;

    public static ComponentType valueOfIfExistsElseUnknown(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ex) {
            return UNKNOWN;
        }
    }
}
