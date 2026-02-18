package ch.admin.bit.jeap.governance.domain.score;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.plugin.rule.Rule;
import ch.admin.bit.jeap.governance.domain.rule.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ComponentScoreCalculatorTest {

    private final ComponentScoreCalculator calculator = new ComponentScoreCalculator();

    private final SystemComponent component = SystemComponent.builder()
            .name("my-service")
            .state(State.OK)
            .type(ComponentType.BACKEND_SERVICE)
            .build();

    private final LocalDate today = LocalDate.of(2026, 2, 17);

    @Test
    void allRulesOk_scoreIs100() {
        var results = List.of(
                okResult("rule-1", 10),
                okResult("rule-2", 5)
        );

        ComponentScore score = calculator.calculateComponentScore(component, today, results);

        assertThat(score.getScore()).isEqualTo(100);
        assertThat(score.getSystemComponent()).isEqualTo(component);
    }

    @Test
    void noRules_scoreIs100() {
        ComponentScore score = calculator.calculateComponentScore(component, today, List.of());

        assertThat(score.getScore()).isEqualTo(100);
    }

    @Test
    void disabledRulesExcludedFromCalculation() {
        var results = List.of(
                okResult("rule-1", 10),
                disabledResult("rule-2", 5)
        );

        ComponentScore score = calculator.calculateComponentScore(component, today, results);

        // Only rule-1 (OK, weight 10) is relevant; rule-2 is disabled and excluded
        assertThat(score.getScore()).isEqualTo(100);
    }

    @Test
    void allRulesDisabled_scoreIs100() {
        var results = List.of(
                disabledResult("rule-1", 10),
                disabledResult("rule-2", 5)
        );

        ComponentScore score = calculator.calculateComponentScore(component, today, results);

        assertThat(score.getScore()).isEqualTo(100);
    }

    @Test
    void mixOfOkAndFailRules_scoreReflectsWeightedRatio() {
        var results = List.of(
                okResult("rule-1", 10),
                failResult("rule-2", 10)
        );

        ComponentScore score = calculator.calculateComponentScore(component, today, results);

        // okWeight=10, totalWeight=20 → 50%
        assertThat(score.getScore()).isEqualTo(50);
    }

    @Test
    void allRulesFailing_scoreIsZero() {
        var results = List.of(
                failResult("rule-1", 10),
                failResult("rule-2", 5)
        );

        ComponentScore score = calculator.calculateComponentScore(component, today, results);

        assertThat(score.getScore()).isEqualTo(0);
    }

    @Test
    void pausedRulesCountTowardsScore() {
        // PAUSED (exempted until) rules are NOT disabled, so they count toward the total weight
        var results = List.of(
                okResult("rule-1", 10),
                pausedResult("rule-2", 10)
        );

        ComponentScore score = calculator.calculateComponentScore(component, today, results);

        // okWeight=10, totalWeight=20 → 50%
        assertThat(score.getScore()).isEqualTo(50);
    }

    @Test
    void weightedScoring_heavierRulesHaveMoreImpact() {
        var results = List.of(
                okResult("important-rule", 90),
                failResult("minor-rule", 10)
        );

        ComponentScore score = calculator.calculateComponentScore(component, today, results);

        // okWeight=90, totalWeight=100 → 90%
        assertThat(score.getScore()).isEqualTo(90);
    }

    private RuleEvaluationResult okResult(String ruleId, int weight) {
        return result(ruleId, weight, State.OK, RuleActivationState.ACTIVE);
    }

    private RuleEvaluationResult failResult(String ruleId, int weight) {
        return result(ruleId, weight, State.FAIL, RuleActivationState.ACTIVE);
    }

    private RuleEvaluationResult disabledResult(String ruleId, int weight) {
        return result(ruleId, weight, State.DISABLED, RuleActivationState.EXEMPTED);
    }

    private RuleEvaluationResult pausedResult(String ruleId, int weight) {
        return result(ruleId, weight, State.PAUSED, RuleActivationState.EXEMPTED_UNTIL);
    }

    private RuleEvaluationResult result(String ruleId, int weight, State state, RuleActivationState activationState) {
        Rule rule = stubRule(ruleId, weight);
        var evaluation = new RuleEvaluation(rule, new RuleParameters(Map.of()), activationState);
        return new RuleEvaluationResult(evaluation, state, null);
    }

    private Rule stubRule(String id, int weight) {
        return new Rule() {
            @Override
            public RuleMetadata metadata() {
                return new RuleMetadata(RuleId.of(id), "Rule " + id, "http://docs/" + id, weight);
            }

            @Override
            public RuleEvaluationResult evaluate(SystemComponent systemComponent, RuleParameters ruleParameters) {
                throw new UnsupportedOperationException("Not used in score tests");
            }
        };
    }
}
