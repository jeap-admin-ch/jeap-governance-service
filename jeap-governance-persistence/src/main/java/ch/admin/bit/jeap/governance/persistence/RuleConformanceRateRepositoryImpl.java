package ch.admin.bit.jeap.governance.persistence;

import ch.admin.bit.jeap.governance.domain.rule.RuleConformanceRate;
import ch.admin.bit.jeap.governance.domain.rule.RuleConformanceRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RuleConformanceRateRepositoryImpl implements RuleConformanceRateRepository {

    private final JpaRuleConformanceRateRepository jpaRuleConformanceRateRepository;

    @Override
    public RuleConformanceRate save(RuleConformanceRate ruleConformanceRate) {
        return jpaRuleConformanceRateRepository.save(ruleConformanceRate);
    }

    @Override
    public Optional<RuleConformanceRate> findByRuleIdAndDay(String ruleId, LocalDate day) {
        return jpaRuleConformanceRateRepository.findByRuleIdAndDay(ruleId, day);
    }

    @Override
    public List<RuleConformanceRate> findByRuleId(String ruleId) {
        return jpaRuleConformanceRateRepository.findByRuleId(ruleId);
    }
}
