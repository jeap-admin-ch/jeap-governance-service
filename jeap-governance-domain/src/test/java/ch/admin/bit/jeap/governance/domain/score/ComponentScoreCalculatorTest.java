package ch.admin.bit.jeap.governance.domain.score;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.rule.RuleEvaluationResult;
import ch.admin.bit.jeap.governance.domain.rule.RuleId;
import ch.admin.bit.jeap.governance.domain.rule.RuleRepository;
import ch.admin.bit.jeap.governance.domain.rule.State;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ComponentScoreCalculatorTest {

    private final RuleRepository ruleRepository = mock(RuleRepository.class);
    private final ComponentScoreCalculator calculator = new ComponentScoreCalculator(ruleRepository);

    private final SystemComponent component = SystemComponent.builder()
            .name("my-service")
            .type(ComponentType.BACKEND_SERVICE)
            .build();

    private final LocalDate today = LocalDate.of(2026, 2, 17);

    @BeforeEach
    void setUp() {
        when(ruleRepository.getActiveRuleWeights()).thenReturn(Map.of(
                RuleId.of("rule-1"), 10,
                RuleId.of("rule-2"), 10,
                RuleId.of("important-rule"), 90,
                RuleId.of("minor-rule"), 10
        ));
    }

    @Test
    void allRulesOk_scoreIs100() {
        var results = List.of(
                okResult("rule-1"),
                okResult("rule-2")
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
                okResult("rule-1"),
                disabledResult("rule-2")
        );

        ComponentScore score = calculator.calculateComponentScore(component, today, results);

        // Only rule-1 (OK, weight 10) is relevant; rule-2 is disabled and excluded
        assertThat(score.getScore()).isEqualTo(100);
    }

    @Test
    void allRulesDisabled_scoreIs100() {
        var results = List.of(
                disabledResult("rule-1"),
                disabledResult("rule-2")
        );

        ComponentScore score = calculator.calculateComponentScore(component, today, results);

        assertThat(score.getScore()).isEqualTo(100);
    }

    @Test
    void mixOfOkAndFailRules_scoreReflectsWeightedRatio() {
        var results = List.of(
                okResult("rule-1"),
                failResult("rule-2")
        );

        ComponentScore score = calculator.calculateComponentScore(component, today, results);

        // okWeight=10, totalWeight=20 → 50%
        assertThat(score.getScore()).isEqualTo(50);
    }

    @Test
    void allRulesFailing_scoreIsZero() {
        var results = List.of(
                failResult("rule-1"),
                failResult("rule-2")
        );

        ComponentScore score = calculator.calculateComponentScore(component, today, results);

        assertThat(score.getScore()).isEqualTo(0);
    }

    @Test
    void pausedRulesCountTowardsScore() {
        // PAUSED (exempted until) rules are NOT disabled, so they count toward the total weight
        var results = List.of(
                okResult("rule-1"),
                pausedResult("rule-2")
        );

        ComponentScore score = calculator.calculateComponentScore(component, today, results);

        // okWeight=10, totalWeight=20 → 50%
        assertThat(score.getScore()).isEqualTo(50);
    }

    @Test
    void weightedScoring_heavierRulesHaveMoreImpact() {
        var results = List.of(
                okResult("important-rule"),
                failResult("minor-rule")
        );

        ComponentScore score = calculator.calculateComponentScore(component, today, results);

        // okWeight=90, totalWeight=100 → 90%
        assertThat(score.getScore()).isEqualTo(90);
    }

    private RuleEvaluationResult okResult(String ruleId) {
        return result(ruleId, State.OK);
    }

    private RuleEvaluationResult failResult(String ruleId) {
        return result(ruleId, State.FAIL);
    }

    private RuleEvaluationResult disabledResult(String ruleId) {
        return result(ruleId, State.DISABLED);
    }

    private RuleEvaluationResult pausedResult(String ruleId) {
        return result(ruleId, State.PAUSED);
    }

    private RuleEvaluationResult result(String ruleId, State state) {
        return new RuleEvaluationResult(RuleId.of(ruleId), state, null);
    }

}
