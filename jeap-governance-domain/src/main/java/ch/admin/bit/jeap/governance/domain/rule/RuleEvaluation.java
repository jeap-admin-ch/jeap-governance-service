package ch.admin.bit.jeap.governance.domain.rule;

import ch.admin.bit.jeap.governance.domain.plugin.rule.Rule;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleParameters;

import java.time.Duration;

/**
 * Bundles a rule with its parameters and activation state for evaluating the
 * rule for a specific component.
 */
public record RuleEvaluation(Rule rule, RuleParameters ruleParameters, RuleActivationState activationState,
                             Duration violationDelay) {

    public RuleEvaluation(Rule rule, RuleParameters ruleParameters, RuleActivationState activationState) {
        this(rule, ruleParameters, activationState, Duration.ZERO);
    }
}
