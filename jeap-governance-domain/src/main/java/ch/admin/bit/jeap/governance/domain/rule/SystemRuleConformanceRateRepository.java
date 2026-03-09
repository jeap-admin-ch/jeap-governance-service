package ch.admin.bit.jeap.governance.domain.rule;

import java.time.LocalDate;
import java.util.List;

public interface SystemRuleConformanceRateRepository {

    void deleteAllByDay(LocalDate day);

    void saveAll(List<SystemRuleConformanceRate> rates);

    List<SystemRuleConformanceRate> findBySystemIdAndDay(long systemId, LocalDate day);

    List<SystemRuleConformanceRate> findLatestPerRuleIdAndSystemId();

    void deleteAllBySystemId(long systemId);
}
