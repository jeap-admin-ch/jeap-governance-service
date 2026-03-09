package ch.admin.bit.jeap.governance.reactionobserver.domain;

import java.util.Optional;

public interface ReactionObserverComponentLastObservationDateRepository {

    Optional<ReactionObserverComponentLastObservationDate> findByComponentId(long id);

    Optional<ReactionObserverComponentLastObservationDate> findByComponentName(String componentName);

    ReactionObserverComponentLastObservationDate add(ReactionObserverComponentLastObservationDate reactionObserverComponentLastObservationDate);

    void delete(ReactionObserverComponentLastObservationDate reactionObserverComponentLastObservationDate);

}
