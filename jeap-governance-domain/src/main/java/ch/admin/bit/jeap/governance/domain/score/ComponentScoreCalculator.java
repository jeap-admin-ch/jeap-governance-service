package ch.admin.bit.jeap.governance.domain.score;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.rule.RuleEvaluationResult;
import ch.admin.bit.jeap.governance.domain.rule.RuleId;
import ch.admin.bit.jeap.governance.domain.rule.RuleRepository;
import ch.admin.bit.jeap.governance.domain.rule.State;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/// Calculates a component's compliance score as a weighted
/// percentage of passing rules. Uses the formular
/// `score = 100 * (sum(ruleWeight)[ruleState == OK] / sum(ruleWeight)[ruleState != DISABLED])`
///  to score components.
@Component
@RequiredArgsConstructor
class ComponentScoreCalculator {

    private final RuleRepository ruleRepository;

    ComponentScore calculateComponentScore(SystemComponent systemComponent, LocalDate day, List<RuleEvaluationResult> allResults) {
        Map<RuleId, Integer> ruleWeights = ruleRepository.getActiveRuleWeights();

        // Disabled (infinitely exempted) rules are irrelevant for the score calculation
        List<RuleEvaluationResult> relevantResults = allResults.stream()
                .filter(result -> result.state() != State.DISABLED)
                .toList();

        int score = calculateScore(relevantResults, ruleWeights);

        return ComponentScore.builder()
                .systemComponent(systemComponent)
                .day(day)
                .score(score)
                .build();
    }

    private int calculateScore(List<RuleEvaluationResult> results, Map<RuleId, Integer> ruleWeights) {
        double totalWeight = totalWeight(results, ruleWeights);
        if (totalWeight == 0) {
            // No rules enabled for this component, so we consider it fully compliant
            return 100;
        }
        double okWeight = ruleWithOkStateWeight(results, ruleWeights);
        return percent(okWeight, totalWeight);
    }

    private static double ruleWithOkStateWeight(List<RuleEvaluationResult> results, Map<RuleId, Integer> ruleWeights) {
        return results.stream()
                .filter(RuleEvaluationResult::isOk)
                .mapToInt(result -> ruleWeights.get(result.ruleId()))
                .reduce(0, Integer::sum);
    }

    private static double totalWeight(List<RuleEvaluationResult> results, Map<RuleId, Integer> ruleWeights) {
        return results.stream()
                .mapToInt(result -> ruleWeights.get(result.ruleId()))
                .reduce(0, Integer::sum);
    }

    private static int percent(double okWeight, double totalWeight) {
        double fraction = okWeight / totalWeight;
        return (int) Math.min(100, fraction * 100);
    }
}
