package ch.admin.bit.jeap.governance.domain.rule;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RuleConformanceRateService {

    private final RuleConformanceRateCalculator ruleConformanceRateCalculator;
    private final RuleConformanceRateRepository ruleConformanceRateRepository;

    @Transactional
    public void updateConformanceRates(List<RuleEvaluationResult> allResults, LocalDate day) {
        List<RuleConformanceRate> rates = ruleConformanceRateCalculator.calculateConformanceRates(allResults, day);
        ruleConformanceRateRepository.deleteAllByDay(day);
        ruleConformanceRateRepository.saveAll(rates);
    }
}
