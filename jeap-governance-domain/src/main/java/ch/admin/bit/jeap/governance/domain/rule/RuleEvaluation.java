package ch.admin.bit.jeap.governance.domain.rule;

public record RuleEvaluation(Rule rule, RuleParameters ruleParameters, RuleActivationState activationState) {
}
