package ch.admin.bit.jeap.governance.web.api.docs.exception;

import lombok.Getter;

@Getter
public class RuleNotFoundException extends RuntimeException {

    public RuleNotFoundException(String message) {
        super(message);
    }

    public static RuleNotFoundException ruleNotFound(String ruleId) {
        return new RuleNotFoundException("No rule with the ID '" + ruleId + "'");
    }


}
