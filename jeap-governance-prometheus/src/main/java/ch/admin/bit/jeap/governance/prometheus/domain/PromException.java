package ch.admin.bit.jeap.governance.prometheus.domain;

import ch.admin.bit.jeap.governance.domain.GovernanceServiceEnvironment;

public class PromException extends RuntimeException {
    
    private PromException(String message, Throwable cause) {
        super(message, cause);
    }

    private PromException(String message) {
        super(message);
    }

    public static PromException connectionFailed(Exception e) {
        return new PromException("Could not connect to Prometheus.", e);
    }

    public static PromException errorResponse(String error) {
        return new PromException("Prometheus returned an error: " + error);
    }

    public static PromException noSample() {
        return new PromException("Prometheus response did not contain a sample.");
    }

    public static PromException allQueryTypesFailedForSystemComponent(String systemComponentName, GovernanceServiceEnvironment environment) {
        return new PromException("All queries to Prometheus failed for the environment '%s' and the system component '%s'."
                .formatted(environment, systemComponentName));
    }

    public static PromException uriProblem(Exception e) {
        return new PromException("Could not determine Prometheus URI.", e);
    }
    
}
