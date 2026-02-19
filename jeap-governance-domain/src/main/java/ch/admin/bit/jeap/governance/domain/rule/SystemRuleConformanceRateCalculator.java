package ch.admin.bit.jeap.governance.domain.rule;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SystemRuleConformanceRateCalculator {

    private final RuleRepository ruleRepository;

    public List<SystemRuleConformanceRate> calculateSystemConformanceRates(long systemId, List<RuleEvaluationResult> systemResults, LocalDate day) {
        Map<RuleId, List<RuleEvaluationResult>> resultsByRule = systemResults.stream()
                .collect(Collectors.groupingBy(RuleEvaluationResult::ruleId));

        List<RuleId> activeRuleIds = ruleRepository.getActiveRuleIds();

        List<SystemRuleConformanceRate> rates = new ArrayList<>();
        for (RuleId ruleId : activeRuleIds) {
            List<RuleEvaluationResult> results = resultsByRule.getOrDefault(ruleId, List.of());
            if (results.isEmpty()) {
                continue;
            }
            long conformantCount = results.stream()
                    .filter(r -> r.state() == State.OK || r.state() == State.DISABLED)
                    .count();
            int rate = (int) (100 * conformantCount / results.size());
            rates.add(SystemRuleConformanceRate.builder()
                    .systemId(systemId)
                    .ruleId(ruleId.id())
                    .conformanceRate(rate)
                    .day(day)
                    .build());
        }
        return rates;
    }
}
