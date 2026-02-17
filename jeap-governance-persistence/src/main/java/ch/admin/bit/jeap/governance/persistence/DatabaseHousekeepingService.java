package ch.admin.bit.jeap.governance.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import static java.time.ZoneOffset.systemDefault;

@Service
@RequiredArgsConstructor
public class DatabaseHousekeepingService {

    private final JpaSystemScoreRepository jpaSystemScoreRepository;
    private final JpaComponentScoreRepository jpaComponentScoreRepository;
    private final JpaRuleConformanceRateRepository jpaRuleConformanceRateRepository;
    private final JpaRuleStateRepository jpaRuleStateRepository;

    @Transactional
    public void performHousekeeping(int maxAgeDays) {
        LocalDate cutoffDate = LocalDate.now().minusDays(maxAgeDays);
        jpaSystemScoreRepository.deleteByDayBefore(cutoffDate);
        jpaComponentScoreRepository.deleteByDayBefore(cutoffDate);
        jpaRuleConformanceRateRepository.deleteByDayBefore(cutoffDate);
        ZonedDateTime cutoffTimestamp = cutoffDate.atStartOfDay(systemDefault());
        jpaRuleStateRepository.deleteByModifiedAtBefore(cutoffTimestamp);
    }
}
