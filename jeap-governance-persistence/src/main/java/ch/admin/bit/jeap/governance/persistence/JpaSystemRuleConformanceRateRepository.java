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

    @Query("SELECT r FROM SystemRuleConformanceRate r WHERE r.createdAt = (" +
            "SELECT MAX(r2.createdAt) FROM SystemRuleConformanceRate r2 " +
            "WHERE r2.ruleId = r.ruleId AND r2.systemId = r.systemId)")
    List<SystemRuleConformanceRate> findLatestPerRuleIdAndSystemId();

    @Modifying
    @Query("DELETE FROM SystemRuleConformanceRate r WHERE r.systemId = :systemId")
    void deleteAllBySystemId(long systemId);
}
