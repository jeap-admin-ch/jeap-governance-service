package ch.admin.bit.jeap.governance.domain.rule;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleInfo;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface RuleRepository {

    List<RuleEvaluation> getRulesToEvaluateForComponent(SystemComponent systemComponent);

    List<RuleId> getActiveRuleIds();

    List<RuleInfo> getActiveRuleInfos();

    Map<RuleId, Integer> getActiveRuleWeights();

    Optional<Map<String, String>> getActiveRuleParameters(RuleId ruleId);
}
