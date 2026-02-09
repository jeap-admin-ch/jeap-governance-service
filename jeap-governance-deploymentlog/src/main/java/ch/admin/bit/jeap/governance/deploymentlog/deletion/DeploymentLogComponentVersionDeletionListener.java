package ch.admin.bit.jeap.governance.deploymentlog.deletion;

import ch.admin.bit.jeap.governance.deploymentlog.DeploymentLogProperties;
import ch.admin.bit.jeap.governance.deploymentlog.domain.DeploymentLogComponentVersion;
import ch.admin.bit.jeap.governance.deploymentlog.domain.DeploymentLogComponentVersionRepository;
import ch.admin.bit.jeap.governance.plugin.api.datasource.ComponentDeletionListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class DeploymentLogComponentVersionDeletionListener implements ComponentDeletionListener {

    private final DeploymentLogComponentVersionRepository repository;

    @Override
    @Transactional
    public void preComponentDeletion(Long systemComponentId) {
        log.debug("Deleting DeploymentLogComponentVersion entities related to system component with ID: {}", systemComponentId);
        Optional<DeploymentLogComponentVersion> byComponentId = repository.findByComponentId(systemComponentId);
        byComponentId.ifPresent(repository::delete);
        log.debug("Deletion done");
    }
}
