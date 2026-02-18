package ch.admin.bit.jeap.governance.domain.score;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.rule.RuleEvaluationResult;
import ch.admin.bit.jeap.governance.domain.rule.State;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/// Calculates a component's compliance score as a weighted
/// percentage of passing rules. Uses the formular
/// `score = 100 * (sum(ruleWeight)[ruleState == OK] / sum(ruleWeight)[ruleState != DISABLED])`
///  to score components.
@Component
class ComponentScoreCalculator {

    ComponentScore calculateComponentScore(SystemComponent systemComponent, LocalDate day, List<RuleEvaluationResult> allResults) {
        // Disabled (infinitely exempted) rules are irrelevant for the score calculation
        List<RuleEvaluationResult> relevantResults = allResults.stream()
                .filter(result -> result.state() != State.DISABLED)
                .toList();

        int score = calculateScore(relevantResults);

        return ComponentScore.builder()
                .systemComponent(systemComponent)
                .day(day)
                .score(score)
                .build();
    }

    private int calculateScore(List<RuleEvaluationResult> results) {
        double totalWeight = totalWeight(results);
        if (totalWeight == 0) {
            // No rules enabled for this component, so we consider it fully compliant
            return 100;
        }
        double okWeight = ruleWithOkStateWeight(results);
        return percent(okWeight, totalWeight);
    }

    private static double ruleWithOkStateWeight(List<RuleEvaluationResult> results) {
        return results.stream()
                .filter(RuleEvaluationResult::isOk)
                .mapToInt(RuleEvaluationResult::ruleWeight)
                .reduce(0, Integer::sum);
    }

    private static double totalWeight(List<RuleEvaluationResult> results) {
        return results.stream()
                .mapToInt(RuleEvaluationResult::ruleWeight)
                .reduce(0, Integer::sum);
    }

    private static int percent(double okWeight, double totalWeight) {
        double fraction = okWeight / totalWeight;
        return (int) Math.min(100, fraction * 100);
    }
}
