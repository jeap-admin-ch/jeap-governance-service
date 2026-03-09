package ch.admin.bit.jeap.governance.persistence;

import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.score.SystemScore;
import ch.admin.bit.jeap.governance.domain.score.SystemScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SystemScoreRepositoryImpl implements SystemScoreRepository {

    private final JpaSystemScoreRepository jpaSystemScoreRepository;

    @Override
    @Transactional
    public SystemScore save(SystemScore systemScore) {
        jpaSystemScoreRepository.deleteBySystemAndDay(systemScore.getSystem(), systemScore.getDay());
        return jpaSystemScoreRepository.save(systemScore);
    }

    @Override
    public Optional<SystemScore> findBySystemAndDay(System system, LocalDate day) {
        return jpaSystemScoreRepository.findBySystemAndDay(system, day);
    }

    @Override
    public List<SystemScore> findBySystem(System system) {
        return jpaSystemScoreRepository.findBySystem(system);
    }

    public List<SystemScore> findAllByDayBetweenInclusive(LocalDate fromDay, LocalDate toDay) {
        return jpaSystemScoreRepository.findAllByDayBetweenInclusive(fromDay, toDay);
    }

    @Override
    public void deleteAllBySystemId(long systemId) {
        jpaSystemScoreRepository.deleteAllBySystemId(systemId);
    }
}
