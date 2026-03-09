package ch.admin.bit.jeap.governance.domain.score;

import ch.admin.bit.jeap.governance.domain.plugin.deletion.ComponentDeletionListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ComponentScoreComponentDeletionListener implements ComponentDeletionListener {

    private final ComponentScoreRepository repository;

    @Override
    @Transactional
    public void preComponentDeletion(long systemComponentId) {
        log.debug("Deleting ComponentScore entities related to system component with ID: {}", systemComponentId);
        repository.deleteAllBySystemComponentId(systemComponentId);
        log.debug("Deletion done");
    }
}
