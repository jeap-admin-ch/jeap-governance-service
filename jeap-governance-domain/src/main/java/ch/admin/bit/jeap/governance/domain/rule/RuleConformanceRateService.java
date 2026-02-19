package ch.admin.bit.jeap.governance.domain.rule;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RuleConformanceRateService {

    private final RuleConformanceRateCalculator ruleConformanceRateCalculator;
    private final RuleConformanceRateRepository ruleConformanceRateRepository;
    private final SystemRuleConformanceRateCalculator systemRuleConformanceRateCalculator;
    private final SystemRuleConformanceRateRepository systemRuleConformanceRateRepository;

    @Transactional
    public void updateConformanceRates(List<RuleEvaluationResult> allResults, LocalDate day) {
        List<RuleConformanceRate> rates = ruleConformanceRateCalculator.calculateConformanceRates(allResults, day);
        ruleConformanceRateRepository.deleteAllByDay(day);
        ruleConformanceRateRepository.saveAll(rates);
    }

    @Transactional
    public void updateSystemConformanceRates(Map<Long, List<RuleEvaluationResult>> resultsBySystem, LocalDate day) {
        systemRuleConformanceRateRepository.deleteAllByDay(day);
        List<SystemRuleConformanceRate> allRates = resultsBySystem.entrySet().stream()
                .flatMap(entry -> systemRuleConformanceRateCalculator
                        .calculateSystemConformanceRates(entry.getKey(), entry.getValue(), day).stream())
                .toList();
        systemRuleConformanceRateRepository.saveAll(allRates);
    }
}
