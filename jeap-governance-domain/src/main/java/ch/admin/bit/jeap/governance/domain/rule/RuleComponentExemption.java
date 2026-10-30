package ch.admin.bit.jeap.governance.domain.rule;

import java.time.LocalDate;
import java.util.Map;

/**
 * A configured exemption of one component from a rule.
 *
 * @param until the last day of a temporary exemption, null for a permanent one
 */
public record RuleComponentExemption(String id,
                                     String componentName,
                                     String reason,
                                     LocalDate until,
                                     Map<String, String> parameters) {

    /**
     * @return the activation state the exemption gives the rule for its component on the given day
     */
    public RuleActivationState activationState(LocalDate today) {
        return RuleActivationState.stateForRuleWithOptionalExemption(today, until);
    }
}
