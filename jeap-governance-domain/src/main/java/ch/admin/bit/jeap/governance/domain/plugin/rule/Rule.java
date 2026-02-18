package ch.admin.bit.jeap.governance.domain.plugin.rule;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.rule.RuleEvaluationResult;
import ch.admin.bit.jeap.governance.domain.rule.RuleMetadata;
import ch.admin.bit.jeap.governance.domain.rule.RuleParameters;

public interface Rule {

    RuleMetadata metadata();

    RuleEvaluationResult evaluate(SystemComponent systemComponent, RuleParameters ruleParameters);

}
