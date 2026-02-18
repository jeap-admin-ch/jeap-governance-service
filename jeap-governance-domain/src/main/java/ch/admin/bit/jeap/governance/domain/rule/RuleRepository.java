package ch.admin.bit.jeap.governance.domain.rule;

import ch.admin.bit.jeap.governance.domain.SystemComponent;

import java.util.List;
import java.util.Map;

public interface RuleRepository {

    List<RuleEvaluation> getRulesToEvaluateForComponent(SystemComponent systemComponent);

    List<RuleId> getActiveRuleIds();

    Map<RuleId, Integer> getActiveRuleWeights();
}
