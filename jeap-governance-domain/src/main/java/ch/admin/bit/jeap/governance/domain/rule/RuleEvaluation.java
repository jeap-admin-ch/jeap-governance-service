package ch.admin.bit.jeap.governance.domain.rule;

import ch.admin.bit.jeap.governance.domain.plugin.rule.Rule;

public record RuleEvaluation(Rule rule, RuleParameters ruleParameters, RuleActivationState activationState) {
}
