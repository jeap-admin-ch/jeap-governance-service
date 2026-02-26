package ch.admin.bit.jeap.governance.domain.plugin.rule;

import ch.admin.bit.jeap.governance.domain.SystemComponent;

/**
 * A governance rule that can be evaluated against a system component to determine compliance.
 */
public interface Rule {

    RuleMetadata metadata();

    RuleResult evaluate(SystemComponent systemComponent, RuleParameters ruleParameters);

    default void validateParameters(RuleParameters ruleParameters) {
    }

}
