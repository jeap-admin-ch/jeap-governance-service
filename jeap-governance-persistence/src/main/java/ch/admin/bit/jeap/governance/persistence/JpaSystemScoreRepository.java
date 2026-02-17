package ch.admin.bit.jeap.governance.persistence;

import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.score.SystemScore;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
interface JpaSystemScoreRepository extends CrudRepository<SystemScore, Long> {

    Optional<SystemScore> findBySystemAndDay(System system, LocalDate day);

    List<SystemScore> findBySystem(System system);

    @Modifying
    @Query("DELETE FROM SystemScore ss WHERE ss.system = :system AND ss.day = :day")
    void deleteBySystemAndDay(@Param("system") System system, @Param("day") LocalDate day);

    @Modifying
    @Query("DELETE FROM SystemScore ss WHERE ss.day < :cutoffDate")
    void deleteByDayBefore(@Param("cutoffDate") LocalDate cutoffDate);
}
