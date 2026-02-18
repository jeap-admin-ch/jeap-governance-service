package ch.admin.bit.jeap.governance.domain.rule;

import ch.admin.bit.jeap.governance.domain.SystemComponent;

import java.util.List;

public interface RuleRepository {

    List<RuleEvaluation> getRulesToEvaluateForComponent(SystemComponent systemComponent);

    List<RuleId> getActiveRuleIds();
}
