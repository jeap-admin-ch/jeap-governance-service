package ch.admin.bit.jeap.governance.persistence;

import ch.admin.bit.jeap.governance.domain.rule.SystemRuleConformanceRate;
import ch.admin.bit.jeap.governance.domain.rule.SystemRuleConformanceRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class SystemRuleConformanceRateRepositoryImpl implements SystemRuleConformanceRateRepository {

    private final JpaSystemRuleConformanceRateRepository jpaSystemRuleConformanceRateRepository;

    @Override
    public void deleteAllByDay(LocalDate day) {
        jpaSystemRuleConformanceRateRepository.deleteByDay(day);
    }

    @Override
    public void saveAll(List<SystemRuleConformanceRate> rates) {
        jpaSystemRuleConformanceRateRepository.saveAll(rates);
    }

    @Override
    public List<SystemRuleConformanceRate> findBySystemIdAndDay(long systemId, LocalDate day) {
        return jpaSystemRuleConformanceRateRepository.findBySystemIdAndDay(systemId, day);
    }

    @Override
    public List<SystemRuleConformanceRate> findLatestPerRuleIdAndSystemId() {
        return jpaSystemRuleConformanceRateRepository.findLatestPerRuleIdAndSystemId();
    }
}
