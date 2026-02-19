package ch.admin.bit.jeap.governance.persistence;

import ch.admin.bit.jeap.governance.domain.rule.SystemRuleConformanceRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
interface JpaSystemRuleConformanceRateRepository extends JpaRepository<SystemRuleConformanceRate, Long> {

    List<SystemRuleConformanceRate> findBySystemIdAndDay(long systemId, LocalDate day);

    @Modifying
    @Query("DELETE FROM SystemRuleConformanceRate r WHERE r.day < :cutoffDate")
    void deleteByDayBefore(@Param("cutoffDate") LocalDate cutoffDate);

    @Modifying
    @Query("DELETE FROM SystemRuleConformanceRate r WHERE r.day = :day")
    void deleteByDay(@Param("day") LocalDate day);
}
