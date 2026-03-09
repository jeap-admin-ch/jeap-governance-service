package ch.admin.bit.jeap.governance.reactionobserver.deletion;

import ch.admin.bit.jeap.governance.domain.plugin.datasource.ComponentDeletionListener;
import ch.admin.bit.jeap.governance.reactionobserver.domain.ReactionObserverComponentLastObservationDate;
import ch.admin.bit.jeap.governance.reactionobserver.domain.ReactionObserverComponentLastObservationDateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ReactionObserverComponentLastObservationDateDeletionListener implements ComponentDeletionListener {

    private final ReactionObserverComponentLastObservationDateRepository repository;

    @Override
    @Transactional
    public void preComponentDeletion(long systemComponentId) {
        log.debug("Deleting ReactionObserverComponentLastObservationDate entities related to system component with ID: {}", systemComponentId);
        Optional<ReactionObserverComponentLastObservationDate> byComponentId = repository.findByComponentId(systemComponentId);
        byComponentId.ifPresent(repository::delete);
        log.debug("Deletion done");
    }
}
