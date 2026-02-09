package ch.admin.bit.jeap.governance.archrepo.deletion;

import ch.admin.bit.jeap.governance.archrepo.domain.ReactionGraph;
import ch.admin.bit.jeap.governance.archrepo.domain.ReactionGraphRepository;
import ch.admin.bit.jeap.governance.plugin.api.datasource.ComponentDeletionListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty("jeap.governance.archrepo.import.reactiongraph.enabled")
@Slf4j
public class ReactionGraphComponentDeletionListener implements ComponentDeletionListener {

    private final ReactionGraphRepository repository;

    @Override
    @Transactional
    public void preComponentDeletion(Long systemComponentId) {
        log.debug("Deleting ReactionGraph entities related to system component with ID: {}", systemComponentId);
        Optional<ReactionGraph> byComponentId = repository.findByComponentId(systemComponentId);
        byComponentId.ifPresent(repository::delete);
        log.debug("Deletion done");
    }
}
