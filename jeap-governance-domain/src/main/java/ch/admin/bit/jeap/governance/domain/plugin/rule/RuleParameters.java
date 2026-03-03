package ch.admin.bit.jeap.governance.domain.plugin.rule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Key-value parameters passed to a rule evaluation, combining rule-specific and exemption-specific parameters. Exemption parameters take precedence.
 */
public record RuleParameters(Map<String, String> parameters) {

    public static RuleParameters ofList(String key, List<String> values) {
        Map<String, String> map = IntStream.range(0, values.size())
                .boxed()
                .collect(Collectors.toMap(
                        i -> key + i,
                        values::get
                ));

        return new RuleParameters(map);
    }

    public static RuleParameters of(Map<String, String> ruleParameters, Map<String, String> exemptionParameters) {
        // Join the two maps, giving precedence to exemptionParameters in case of key conflicts
        Map<String, String> combinedParameters = new HashMap<>(ruleParameters);
        combinedParameters.putAll(exemptionParameters);
        return new RuleParameters(combinedParameters);
    }

    public List<String> getParameterAsList(String key) {
        return parameters.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(key))
                .map(Map.Entry::getValue)
                .toList();
    }
}
