package ch.admin.bit.jeap.governance.domain.rule;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SystemRuleConformanceRateCalculatorTest {

    private static final LocalDate DAY = LocalDate.of(2025, 6, 15);
    private static final long SYSTEM_ID = 42L;

    private final RuleRepository ruleRepository = mock(RuleRepository.class);
    private final SystemRuleConformanceRateCalculator calculator = new SystemRuleConformanceRateCalculator(ruleRepository);

    @Test
    void allOk_returns100() {
        when(ruleRepository.getActiveRuleIds()).thenReturn(List.of(RuleId.of("rule-1")));
        var results = List.of(
                result("rule-1", State.OK),
                result("rule-1", State.OK)
        );

        var rates = calculator.calculateSystemConformanceRates(SYSTEM_ID, results, DAY);

        assertThat(rates).hasSize(1);
        assertThat(rates.getFirst().getSystemId()).isEqualTo(SYSTEM_ID);
        assertThat(rates.getFirst().getRuleId()).isEqualTo("rule-1");
        assertThat(rates.getFirst().getConformanceRate()).isEqualTo(100);
    }

    @Test
    void allFail_returns0() {
        when(ruleRepository.getActiveRuleIds()).thenReturn(List.of(RuleId.of("rule-1")));
        var results = List.of(
                result("rule-1", State.FAIL),
                result("rule-1", State.FAIL)
        );

        var rates = calculator.calculateSystemConformanceRates(SYSTEM_ID, results, DAY);

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

        var rates = calculator.calculateSystemConformanceRates(SYSTEM_ID, results, DAY);

        assertThat(rates).hasSize(1);
        assertThat(rates.getFirst().getConformanceRate()).isEqualTo(66);
    }

    @Test
    void disabledCountedAsConformant() {
        when(ruleRepository.getActiveRuleIds()).thenReturn(List.of(RuleId.of("rule-1")));
        var results = List.of(
                result("rule-1", State.OK),
                result("rule-1", State.DISABLED)
        );

        var rates = calculator.calculateSystemConformanceRates(SYSTEM_ID, results, DAY);

        assertThat(rates).hasSize(1);
        assertThat(rates.getFirst().getConformanceRate()).isEqualTo(100);
    }

    @Test
    void pausedCountedAsNonConformant() {
        when(ruleRepository.getActiveRuleIds()).thenReturn(List.of(RuleId.of("rule-1")));
        var results = List.of(
                result("rule-1", State.OK),
                result("rule-1", State.PAUSED)
        );

        var rates = calculator.calculateSystemConformanceRates(SYSTEM_ID, results, DAY);

        assertThat(rates).hasSize(1);
        assertThat(rates.getFirst().getConformanceRate()).isEqualTo(50);
    }

    @Test
    void noResults_noRateCreated() {
        when(ruleRepository.getActiveRuleIds()).thenReturn(List.of(RuleId.of("rule-1")));

        var rates = calculator.calculateSystemConformanceRates(SYSTEM_ID, List.of(), DAY);

        assertThat(rates).isEmpty();
    }

    @Test
    void noActiveRules_returnsEmptyList() {
        when(ruleRepository.getActiveRuleIds()).thenReturn(List.of());

        var rates = calculator.calculateSystemConformanceRates(SYSTEM_ID, List.of(), DAY);

        assertThat(rates).isEmpty();
    }

    @Test
    void ruleWithNoResultsForSystem_noRateCreated() {
        when(ruleRepository.getActiveRuleIds()).thenReturn(List.of(RuleId.of("rule-1"), RuleId.of("rule-2")));
        var results = List.of(
                result("rule-1", State.OK)
        );

        var rates = calculator.calculateSystemConformanceRates(SYSTEM_ID, results, DAY);

        assertThat(rates).hasSize(1);
        assertThat(rates.getFirst().getRuleId()).isEqualTo("rule-1");
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

        var rates = calculator.calculateSystemConformanceRates(SYSTEM_ID, results, DAY);

        assertThat(rates).hasSize(2);
        var rule1Rate = rates.stream().filter(r -> r.getRuleId().equals("rule-1")).findFirst().orElseThrow();
        var rule2Rate = rates.stream().filter(r -> r.getRuleId().equals("rule-2")).findFirst().orElseThrow();
        assertThat(rule1Rate.getConformanceRate()).isEqualTo(50);
        assertThat(rule2Rate.getConformanceRate()).isEqualTo(100);
    }

    @Test
    void mixedOkFailDisabled_countsOkAndDisabledAsConformant() {
        when(ruleRepository.getActiveRuleIds()).thenReturn(List.of(RuleId.of("rule-1")));
        var results = List.of(
                result("rule-1", State.OK),
                result("rule-1", State.FAIL),
                result("rule-1", State.DISABLED)
        );

        var rates = calculator.calculateSystemConformanceRates(SYSTEM_ID, results, DAY);

        assertThat(rates).hasSize(1);
        // 2 conformant (OK + DISABLED) out of 3 total = 66%
        assertThat(rates.getFirst().getConformanceRate()).isEqualTo(66);
    }

    private static RuleEvaluationResult result(String ruleId, State state) {
        return new RuleEvaluationResult(RuleId.of(ruleId), state, null);
    }
}
