package ch.admin.bit.jeap.governance.persistence;

import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.score.ComponentScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
interface JpaComponentScoreRepository extends JpaRepository<ComponentScore, Long> {

    Optional<ComponentScore> findBySystemComponentAndDay(SystemComponent systemComponent, LocalDate day);

    List<ComponentScore> findBySystemComponent(SystemComponent systemComponent);

    @Modifying
    @Query("DELETE FROM ComponentScore cs WHERE cs.systemComponent.system = :system AND cs.day = :day")
    void deleteBySystemAndDay(@Param("system") System system, @Param("day") LocalDate day);

    @Modifying
    @Query("DELETE FROM ComponentScore cs WHERE cs.day < :cutoffDate")
    void deleteByDayBefore(@Param("cutoffDate") LocalDate cutoffDate);
}
