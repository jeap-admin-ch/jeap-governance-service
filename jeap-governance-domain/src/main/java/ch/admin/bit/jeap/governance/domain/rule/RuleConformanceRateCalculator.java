package ch.admin.bit.jeap.governance.domain.rule;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RuleConformanceRateCalculator {

    private final RuleRepository ruleRepository;

    public List<RuleConformanceRate> calculateConformanceRates(List<RuleEvaluationResult> allResults, LocalDate day) {
        Map<RuleId, List<RuleEvaluationResult>> resultsByRule = allResults.stream()
                .filter(result -> result.state() != State.DISABLED)
                .collect(Collectors.groupingBy(RuleEvaluationResult::ruleId));

        Set<RuleId> activeRuleIds = Set.copyOf(ruleRepository.getActiveRuleIds());

        return activeRuleIds.stream()
                .map(ruleId -> toConformanceRate(ruleId, resultsByRule.getOrDefault(ruleId, List.of()), day))
                .toList();
    }

    private RuleConformanceRate toConformanceRate(RuleId ruleId, List<RuleEvaluationResult> results, LocalDate day) {
        int rate = 0;
        if (!results.isEmpty()) {
            long okCount = results.stream().filter(RuleEvaluationResult::isOk).count();
            rate = (int) (100 * okCount / results.size());
        }

        return RuleConformanceRate.builder()
                .ruleId(ruleId.id())
                .conformanceRate(rate)
                .day(day)
                .build();
    }
}
