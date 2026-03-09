package ch.admin.bit.jeap.governance.reactionobserver.persistence;

import ch.admin.bit.jeap.governance.reactionobserver.domain.ReactionObserverComponentLastObservationDate;
import ch.admin.bit.jeap.governance.reactionobserver.domain.ReactionObserverComponentLastObservationDateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReactionObserverComponentLastObservationDateRepositoryImpl implements ReactionObserverComponentLastObservationDateRepository {

    private final JpaReactionObserverComponentLastObservationDateRepository jpaRepository;

    @Override
    public Optional<ReactionObserverComponentLastObservationDate> findByComponentId(long componentId) {
        return jpaRepository.findBySystemComponentId(componentId);
    }

    @Override
    public Optional<ReactionObserverComponentLastObservationDate> findByComponentName(String componentName) {
        return jpaRepository.findBySystemComponentName(componentName);
    }

    @Override
    public ReactionObserverComponentLastObservationDate add(ReactionObserverComponentLastObservationDate reactionObserverComponentLastObservationDate) {
        return jpaRepository.save(reactionObserverComponentLastObservationDate);
    }

    @Override
    public void delete(ReactionObserverComponentLastObservationDate reactionObserverComponentLastObservationDate) {
        jpaRepository.delete(reactionObserverComponentLastObservationDate);
    }
}
