package ch.admin.bit.jeap.governance.persistence;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.rule.RuleState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.Optional;

@Repository
interface JpaRuleStateRepository extends JpaRepository<RuleState, Long> {

    Optional<RuleState> findBySystemComponentAndRuleId(SystemComponent systemComponent, String ruleId);

    @Modifying
    @Query("DELETE FROM RuleState rs WHERE rs.modifiedAt < :cutoffTimestamp")
    void deleteByModifiedAtBefore(@Param("cutoffTimestamp") ZonedDateTime cutoffTimestamp);
}
