package ch.admin.bit.jeap.governance.reactionobserver.persistence;

import ch.admin.bit.jeap.governance.reactionobserver.domain.ReactionObserverComponentLastObservationDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaReactionObserverComponentLastObservationDateRepository extends JpaRepository<ReactionObserverComponentLastObservationDate, Long> {

    Optional<ReactionObserverComponentLastObservationDate> findBySystemComponentName(String componentName);

    Optional<ReactionObserverComponentLastObservationDate> findBySystemComponentId(long componentId);
}
