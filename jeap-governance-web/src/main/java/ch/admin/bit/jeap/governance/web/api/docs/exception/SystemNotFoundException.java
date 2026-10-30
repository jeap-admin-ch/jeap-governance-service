package ch.admin.bit.jeap.governance.web.api.docs.exception;

import lombok.Getter;

@Getter
public class SystemNotFoundException extends RuntimeException {

    public SystemNotFoundException(String message) {
        super(message);
    }

    public static SystemNotFoundException systemNotFound(String systemId) {
        return new SystemNotFoundException("No system with the ID '" + systemId + "'");
    }


}
