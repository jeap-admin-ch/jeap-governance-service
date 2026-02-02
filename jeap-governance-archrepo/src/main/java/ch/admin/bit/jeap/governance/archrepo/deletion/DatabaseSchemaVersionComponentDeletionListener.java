package ch.admin.bit.jeap.governance.archrepo.deletion;

import ch.admin.bit.jeap.governance.archrepo.domain.DatabaseSchemaVersion;
import ch.admin.bit.jeap.governance.archrepo.domain.DatabaseSchemaVersionRepository;
import ch.admin.bit.jeap.governance.plugin.api.datasource.ComponentDeletionListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty("jeap.governance.archrepo.import.databaseschemaversion.enabled")
@Slf4j
public class DatabaseSchemaVersionComponentDeletionListener implements ComponentDeletionListener {

    private final DatabaseSchemaVersionRepository repository;

    @Override
    @Transactional
    public void preComponentDeletion(UUID systemComponentId) {
        log.debug("Deleting DatabaseSchemaVersion entities related to system component with ID: {}", systemComponentId);
        Optional<DatabaseSchemaVersion> byComponentId = repository.findByComponentId(systemComponentId);
        byComponentId.ifPresent(repository::delete);
        log.debug("Deletion done");
    }
}
