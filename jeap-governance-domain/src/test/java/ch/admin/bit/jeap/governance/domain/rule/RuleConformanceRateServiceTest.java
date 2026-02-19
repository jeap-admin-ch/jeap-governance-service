package ch.admin.bit.jeap.governance.domain.rule;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleConformanceRateServiceTest {

    private static final LocalDate DAY = LocalDate.of(2025, 6, 15);

    @Mock
    private RuleConformanceRateCalculator ruleConformanceRateCalculator;
    @Mock
    private RuleConformanceRateRepository ruleConformanceRateRepository;
    @Mock
    private SystemRuleConformanceRateCalculator systemRuleConformanceRateCalculator;
    @Mock
    private SystemRuleConformanceRateRepository systemRuleConformanceRateRepository;

    @InjectMocks
    private RuleConformanceRateService service;

    @Test
    void updateConformanceRates_deletesAndSaves() {
        List<RuleEvaluationResult> results = List.of(
                new RuleEvaluationResult(RuleId.of("rule-1"), State.OK, null)
        );
        var rate = RuleConformanceRate.builder().ruleId("rule-1").conformanceRate(100).day(DAY).build();
        when(ruleConformanceRateCalculator.calculateConformanceRates(results, DAY)).thenReturn(List.of(rate));

        service.updateConformanceRates(results, DAY);

        verify(ruleConformanceRateRepository).deleteAllByDay(DAY);
        verify(ruleConformanceRateRepository).saveAll(List.of(rate));
    }

    @Test
    void updateSystemConformanceRates_deletesAndSavesForAllSystems() {
        var results1 = List.of(new RuleEvaluationResult(RuleId.of("rule-1"), State.OK, null));
        var results2 = List.of(new RuleEvaluationResult(RuleId.of("rule-1"), State.FAIL, null));

        var rate1 = SystemRuleConformanceRate.builder().systemId(1L).ruleId("rule-1").conformanceRate(100).day(DAY).build();
        var rate2 = SystemRuleConformanceRate.builder().systemId(2L).ruleId("rule-1").conformanceRate(0).day(DAY).build();
        when(systemRuleConformanceRateCalculator.calculateSystemConformanceRates(1L, results1, DAY))
                .thenReturn(List.of(rate1));
        when(systemRuleConformanceRateCalculator.calculateSystemConformanceRates(2L, results2, DAY))
                .thenReturn(List.of(rate2));

        service.updateSystemConformanceRates(Map.of(1L, results1, 2L, results2), DAY);

        verify(systemRuleConformanceRateRepository).deleteAllByDay(DAY);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SystemRuleConformanceRate>> captor = ArgumentCaptor.forClass(List.class);
        verify(systemRuleConformanceRateRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder(rate1, rate2);
    }
}
