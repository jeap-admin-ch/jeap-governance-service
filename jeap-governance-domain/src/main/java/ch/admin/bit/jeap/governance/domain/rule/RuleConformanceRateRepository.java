package ch.admin.bit.jeap.governance.domain.rule;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RuleConformanceRateRepository {

    RuleConformanceRate save(RuleConformanceRate ruleConformanceRate);

    Optional<RuleConformanceRate> findByRuleIdAndDay(String ruleId, LocalDate day);

    List<RuleConformanceRate> findByRuleId(String ruleId);
}
