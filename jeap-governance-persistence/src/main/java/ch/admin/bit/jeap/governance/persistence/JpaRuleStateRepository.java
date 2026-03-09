package ch.admin.bit.jeap.governance.persistence;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.rule.NonCompliantComponentEntry;
import ch.admin.bit.jeap.governance.domain.rule.RuleState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Repository
interface JpaRuleStateRepository extends JpaRepository<RuleState, Long> {

    Optional<RuleState> findBySystemComponentAndRuleId(SystemComponent systemComponent, String ruleId);

    @Modifying
    @Query("DELETE FROM RuleState rs WHERE rs.modifiedAt < :cutoffTimestamp")
    void deleteByModifiedAtBefore(@Param("cutoffTimestamp") ZonedDateTime cutoffTimestamp);

    @Query("""
            SELECT rs.systemComponent.system.id   AS systemId,
                   rs.systemComponent.id          AS systemComponentId,
                   rs.systemComponent.name        AS systemComponentName,
                   rs.ruleId                      AS ruleId,
                   rs.stateComment                AS stateComment,
                   rs.modifiedAt                  AS nonCompliantSince
            FROM RuleState rs
            WHERE rs.state = ch.admin.bit.jeap.governance.domain.rule.State.FAIL
            """)
    List<NonCompliantComponentEntry> findNonCompliantSince();

    @Modifying
    @Query("DELETE FROM RuleState rs WHERE rs.systemComponent.id = :systemComponentId")
    void deleteAllBySystemComponentId(long systemComponentId);
}
