package ch.admin.bit.jeap.governance.domain.rule;

import ch.admin.bit.jeap.governance.domain.SystemComponent;

public interface Rule {

    RuleMetadata metadata();

    RuleEvaluationResult evaluate(SystemComponent systemComponent, RuleParameters ruleParameters);

}
