package ch.admin.bit.jeap.governance.persistence;

import ch.admin.bit.jeap.governance.domain.rule.RuleConformanceRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
interface JpaRuleConformanceRateRepository extends JpaRepository<RuleConformanceRate, Long> {

    Optional<RuleConformanceRate> findByRuleIdAndDay(String ruleId, LocalDate day);

    List<RuleConformanceRate> findByRuleId(String ruleId);

    @Modifying
    @Query("DELETE FROM RuleConformanceRate rcr WHERE rcr.day < :cutoffDate")
    void deleteByDayBefore(@Param("cutoffDate") LocalDate cutoffDate);

    @Modifying
    @Query("DELETE FROM RuleConformanceRate rcr WHERE rcr.day = :day")
    void deleteByDay(@Param("day") LocalDate day);

    @Query("SELECT rcr FROM RuleConformanceRate rcr WHERE rcr.day >= :fromDay AND rcr.day <= :toDay")
    List<RuleConformanceRate> findAllByDayBetweenInclusive(LocalDate fromDay, LocalDate toDay);
}
