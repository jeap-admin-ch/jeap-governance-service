package ch.admin.bit.jeap.governance.domain.score;

import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemComponent;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ComponentScoreRepository {

    ComponentScore save(ComponentScore componentScore);

    Optional<ComponentScore> findBySystemComponentAndDay(SystemComponent systemComponent, LocalDate day);

    List<ComponentScore> findBySystemComponent(SystemComponent systemComponent);

    void saveOrReplaceAllForSystemAndDay(System system, List<ComponentScore> componentScores, LocalDate day);

    List<ComponentScore> findAllByDayBetweenInclusive(LocalDate fromDay, LocalDate toDay);

    void deleteAllBySystemComponentId(long componentId);
}
