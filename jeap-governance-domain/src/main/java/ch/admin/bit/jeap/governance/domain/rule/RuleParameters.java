package ch.admin.bit.jeap.governance.domain.rule;

import java.util.HashMap;
import java.util.Map;

public record RuleParameters(Map<String, String> parameters) {

    public static RuleParameters of(Map<String, String> ruleParameters, Map<String, String> exemptionParameters) {
        // Join the two maps, giving precedence to exemptionParameters in case of key conflicts
        Map<String, String> combinedParameters = new HashMap<>(ruleParameters);
        combinedParameters.putAll(exemptionParameters);
        return new RuleParameters(combinedParameters);
    }
}
