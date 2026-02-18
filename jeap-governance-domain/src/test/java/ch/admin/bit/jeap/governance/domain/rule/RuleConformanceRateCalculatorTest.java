package ch.admin.bit.jeap.governance.domain.rule;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.plugin.rule.Rule;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RuleConformanceRateCalculatorTest {

    private static final LocalDate DAY = LocalDate.of(2025, 6, 15);

    private final RuleRepository ruleRepository = mock(RuleRepository.class);
    private final RuleConformanceRateCalculator calculator = new RuleConformanceRateCalculator(ruleRepository);

    @Test
    void allOk_returns100() {
        when(ruleRepository.getActiveRuleIds()).thenReturn(List.of(RuleId.of("rule-1")));
        var results = List.of(
                result("rule-1", State.OK),
                result("rule-1", State.OK)
        );

        var rates = calculator.calculateConformanceRates(results, DAY);

        assertThat(rates).hasSize(1);
        assertThat(rates.getFirst().getConformanceRate()).isEqualTo(100);
        assertThat(rates.getFirst().getRuleId()).isEqualTo("rule-1");
    }

    @Test
    void allFail_returns0() {
        when(ruleRepository.getActiveRuleIds()).thenReturn(List.of(RuleId.of("rule-1")));
        var results = List.of(
                result("rule-1", State.FAIL),
                result("rule-1", State.FAIL)
        );

        var rates = calculator.calculateConformanceRates(results, DAY);

        assertThat(rates).hasSize(1);
        assertThat(rates.getFirst().getConformanceRate()).isZero();
    }

    @Test
    void mixed_returnsCorrectPercentage() {
        when(ruleRepository.getActiveRuleIds()).thenReturn(List.of(RuleId.of("rule-1")));
        var results = List.of(
                result("rule-1", State.OK),
                result("rule-1", State.FAIL),
                result("rule-1", State.OK)
        );

        var rates = calculator.calculateConformanceRates(results, DAY);

        assertThat(rates).hasSize(1);
        assertThat(rates.getFirst().getConformanceRate()).isEqualTo(66);
    }

    @Test
    void disabledExcluded() {
        when(ruleRepository.getActiveRuleIds()).thenReturn(List.of(RuleId.of("rule-1")));
        var results = List.of(
                result("rule-1", State.OK),
                result("rule-1", State.DISABLED)
        );

        var rates = calculator.calculateConformanceRates(results, DAY);

        assertThat(rates).hasSize(1);
        assertThat(rates.getFirst().getConformanceRate()).isEqualTo(100);
    }

    @Test
    void allDisabled_producesZeroRate() {
        when(ruleRepository.getActiveRuleIds()).thenReturn(List.of(RuleId.of("rule-1")));
        var results = List.of(
                result("rule-1", State.DISABLED),
                result("rule-1", State.DISABLED)
        );

        var rates = calculator.calculateConformanceRates(results, DAY);

        assertThat(rates).hasSize(1);
        assertThat(rates.getFirst().getConformanceRate()).isZero();
    }

    @Test
    void pausedCountedAsNonConformant() {
        when(ruleRepository.getActiveRuleIds()).thenReturn(List.of(RuleId.of("rule-1")));
        var results = List.of(
                result("rule-1", State.OK),
                result("rule-1", State.PAUSED)
        );

        var rates = calculator.calculateConformanceRates(results, DAY);

        assertThat(rates).hasSize(1);
        assertThat(rates.getFirst().getConformanceRate()).isEqualTo(50);
    }

    @Test
    void noResults_activeRuleGetsZeroRate() {
        when(ruleRepository.getActiveRuleIds()).thenReturn(List.of(RuleId.of("rule-1")));

        var rates = calculator.calculateConformanceRates(List.of(), DAY);

        assertThat(rates).hasSize(1);
        assertThat(rates.getFirst().getRuleId()).isEqualTo("rule-1");
        assertThat(rates.getFirst().getConformanceRate()).isZero();
    }

    @Test
    void noResults_noActiveRules_returnsEmptyList() {
        when(ruleRepository.getActiveRuleIds()).thenReturn(List.of());

        var rates = calculator.calculateConformanceRates(List.of(), DAY);

        assertThat(rates).isEmpty();
    }

    @Test
    void multipleRulesAggregatedIndependently() {
        when(ruleRepository.getActiveRuleIds()).thenReturn(List.of(RuleId.of("rule-1"), RuleId.of("rule-2")));
        var results = List.of(
                result("rule-1", State.OK),
                result("rule-1", State.FAIL),
                result("rule-2", State.OK),
                result("rule-2", State.OK)
        );

        var rates = calculator.calculateConformanceRates(results, DAY);

        assertThat(rates).hasSize(2);
        var rule1Rate = rates.stream().filter(r -> r.getRuleId().equals("rule-1")).findFirst().orElseThrow();
        var rule2Rate = rates.stream().filter(r -> r.getRuleId().equals("rule-2")).findFirst().orElseThrow();
        assertThat(rule1Rate.getConformanceRate()).isEqualTo(50);
        assertThat(rule2Rate.getConformanceRate()).isEqualTo(100);
    }

    private static RuleEvaluationResult result(String ruleId, State state) {
        Rule rule = new Rule() {
            @Override
            public RuleMetadata metadata() {
                return new RuleMetadata(RuleId.of(ruleId), "Rule", null, 10);
            }

            @Override
            public RuleEvaluationResult evaluate(SystemComponent systemComponent, RuleParameters ruleParameters) {
                throw new UnsupportedOperationException();
            }
        };
        var evaluation = new RuleEvaluation(rule, new RuleParameters(Map.of()), RuleActivationState.ACTIVE);
        return new RuleEvaluationResult(evaluation, state, null);
    }
}
