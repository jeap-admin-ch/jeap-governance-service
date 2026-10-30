package ch.admin.bit.jeap.governance.web.api.docs.exception;

public class SystemNotFoundException extends RuntimeException {

    public SystemNotFoundException(String message) {
        super(message);
    }

    public static SystemNotFoundException systemNotFound(String systemName) {
        return new SystemNotFoundException("No scored system with the name '" + systemName + "'");
    }
}
