package ch.admin.bit.jeap.governance.persistence;

import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.score.ComponentScore;
import ch.admin.bit.jeap.governance.domain.score.ComponentScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ComponentScoreRepositoryImpl implements ComponentScoreRepository {

    private final JpaComponentScoreRepository jpaComponentScoreRepository;

    @Override
    public ComponentScore save(ComponentScore componentScore) {
        return jpaComponentScoreRepository.save(componentScore);
    }

    @Override
    public Optional<ComponentScore> findBySystemComponentAndDay(SystemComponent systemComponent, LocalDate day) {
        return jpaComponentScoreRepository.findBySystemComponentAndDay(systemComponent, day);
    }

    @Override
    public List<ComponentScore> findBySystemComponent(SystemComponent systemComponent) {
        return jpaComponentScoreRepository.findBySystemComponent(systemComponent);
    }

    @Override
    @Transactional
    public void saveOrReplaceAllForSystemAndDay(System system, List<ComponentScore> componentScores, LocalDate day) {
        jpaComponentScoreRepository.deleteBySystemAndDay(system, day);
        jpaComponentScoreRepository.saveAll(componentScores);
    }

    @Override
    public List<ComponentScore> findAllByDayBetweenInclusive(LocalDate fromDay, LocalDate toDay) {
        return jpaComponentScoreRepository.findAllByDayBetweenInclusive(fromDay, toDay);
    }

    @Override
    public void deleteAllBySystemComponentId(long componentId) {
        jpaComponentScoreRepository.deleteAllBySystemComponentId(componentId);
    }
}
