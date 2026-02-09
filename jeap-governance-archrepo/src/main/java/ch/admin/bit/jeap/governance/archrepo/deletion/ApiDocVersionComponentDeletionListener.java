package ch.admin.bit.jeap.governance.archrepo.deletion;

import ch.admin.bit.jeap.governance.archrepo.domain.ApiDocVersion;
import ch.admin.bit.jeap.governance.archrepo.domain.ApiDocVersionRepository;
import ch.admin.bit.jeap.governance.plugin.api.datasource.ComponentDeletionListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty("jeap.governance.archrepo.import.apidocversion.enabled")
@Slf4j
public class ApiDocVersionComponentDeletionListener implements ComponentDeletionListener {

    private final ApiDocVersionRepository repository;

    @Override
    @Transactional
    public void preComponentDeletion(Long systemComponentId) {
        log.debug("Deleting ApiDocVersion entities related to system component with ID: {}", systemComponentId);
        Optional<ApiDocVersion> byComponentId = repository.findByComponentId(systemComponentId);
        byComponentId.ifPresent(repository::delete);
        log.debug("Deletion done");
    }
}
