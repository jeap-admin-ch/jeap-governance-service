package ch.admin.bit.jeap.governance.secscan.persistence;

import ch.admin.bit.jeap.governance.secscan.domain.SecscanState;
import ch.admin.bit.jeap.governance.secscan.domain.SecscanStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
@RequiredArgsConstructor
class SecscanStateRepositoryImpl implements SecscanStateRepository {

    private final SecscanStateJpaRepository secscanStateJpaRepository;

    @Override
    public SecscanState save(SecscanState secscanState) {
        return secscanStateJpaRepository.save(secscanState);
    }

    @Override
    public Optional<SecscanState> findBySystemComponentId(long systemComponentId) {
        return secscanStateJpaRepository.findBySystemComponentId(systemComponentId);
    }

    @Override
    public int deleteBySystemComponentId(long systemComponentId) {
        return secscanStateJpaRepository.deleteBySystemComponentId(systemComponentId);
    }

}
