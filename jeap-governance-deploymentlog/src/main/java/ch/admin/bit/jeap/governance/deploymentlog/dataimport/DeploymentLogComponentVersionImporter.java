package ch.admin.bit.jeap.governance.deploymentlog.dataimport;

import ch.admin.bit.jeap.governance.deploymentlog.connector.DeploymentLogConnector;
import ch.admin.bit.jeap.governance.deploymentlog.connector.model.DeploymentLogComponentVersionDto;
import ch.admin.bit.jeap.governance.deploymentlog.synchronize.DeploymentLogComponentVersionSynchronizer;
import ch.admin.bit.jeap.governance.domain.GovernanceProperties;
import ch.admin.bit.jeap.governance.plugin.api.datasource.DataSourceImporter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Set;

import static ch.admin.bit.jeap.governance.deploymentlog.dataimport.ImportOrder.DEPLOYMENT_LOG_COMPONENT_VERSION_IMPORT_ORDER;

@Component
@Order(DEPLOYMENT_LOG_COMPONENT_VERSION_IMPORT_ORDER)
@RequiredArgsConstructor
@Slf4j
public class DeploymentLogComponentVersionImporter implements DataSourceImporter {

    private final GovernanceProperties properties;
    private final DeploymentLogConnector connector;
    private final DeploymentLogComponentVersionSynchronizer synchronizer;

    @Override
    public void importData() {
        log.info("Start synchronization with deployment log");
        Set<DeploymentLogComponentVersionDto> dtos = connector.getAllComponentVersions(properties.getEnvironment());
        log.debug("Got model from deployment log: {}", dtos);
        synchronizer.synchronizeModelWithDeploymentLog(dtos);
        log.info("Finished synchronization with deployment log");
    }
}
