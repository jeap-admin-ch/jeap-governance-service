package ch.admin.bit.jeap.governance.domain.score;

import ch.admin.bit.jeap.governance.domain.System;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SystemScoreRepository {

    SystemScore save(SystemScore systemScore);

    Optional<SystemScore> findBySystemAndDay(System system, LocalDate day);

    List<SystemScore> findBySystem(System system);

    List<SystemScore> findAllByDayBetweenInclusive(LocalDate fromDay, LocalDate toDay);
}
