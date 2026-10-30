package ch.admin.bit.jeap.governance.domain.rule;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleInfo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface RuleRepository {

    List<RuleEvaluation> getRulesToEvaluateForComponent(SystemComponent systemComponent);

    List<RuleId> getActiveRuleIds();

    List<RuleInfo> getActiveRuleInfos();

    Map<RuleId, Integer> getActiveRuleWeights();

    Optional<Map<String, String>> getActiveRuleParameters(RuleId ruleId);

    /**
     * @return every rule the governance service knows, active or not, sorted by rule ID
     */
    List<RuleDescription> getKnownRules();

    /**
     * The nominal share of each active rule: its weight divided by the sum of the weights of all active rules, in
     * percent and rounded to one decimal. Computed from {@link #getActiveRuleWeights()}, the weights the scoring
     * uses. On a component for which other rules are disabled, a rule weighs more than its nominal share.
     */
    default Map<RuleId, BigDecimal> getActiveRuleShares() {
        Map<RuleId, Integer> weights = getActiveRuleWeights();
        int totalWeight = weights.values().stream().mapToInt(Integer::intValue).sum();
        Map<RuleId, BigDecimal> shares = new LinkedHashMap<>();
        weights.forEach((ruleId, weight) -> shares.put(ruleId, totalWeight == 0 ? BigDecimal.ZERO :
                BigDecimal.valueOf(100L * weight).divide(BigDecimal.valueOf(totalWeight), 1, RoundingMode.HALF_UP)));
        return shares;
    }
}
