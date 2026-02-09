package ch.admin.bit.jeap.governance.deploymentlog.synchronize;

import ch.admin.bit.jeap.governance.deploymentlog.connector.model.DeploymentLogComponentVersionDto;
import ch.admin.bit.jeap.governance.deploymentlog.domain.DeploymentLogComponentVersion;
import ch.admin.bit.jeap.governance.deploymentlog.domain.DeploymentLogComponentVersionRepository;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.SystemComponentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeploymentLogComponentVersionOneByOneSynchronizer {

    private final SystemComponentRepository systemComponentRepository;
    private final DeploymentLogComponentVersionRepository deploymentLogComponentVersionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void synchronize(DeploymentLogComponentVersionDto dto) {
        Optional<SystemComponent> systemComponentOptional = systemComponentRepository.findByName(dto.componentName());
        if (systemComponentOptional.isEmpty()) {
            log.warn("Received component version for unknown system component '{}', skipping synchronization", dto.componentName());
            return;
        }
        synchronize(systemComponentOptional.get(), dto);
    }

    private void synchronize(SystemComponent systemComponent, DeploymentLogComponentVersionDto dto) {
        Optional<DeploymentLogComponentVersion> entityOptional = deploymentLogComponentVersionRepository.findByComponentName(dto.componentName());

        entityOptional.ifPresent(deploymentLogComponentVersionRepository::delete);
        log.info("Creating new DeploymentLogComponentVersionDto for system component {}: {}", systemComponent.getName(), dto);
        DeploymentLogComponentVersion newVersion = DeploymentLogComponentVersion.builder()
                .systemComponent(systemComponent)
                .version(dto.version())
                .build();
        deploymentLogComponentVersionRepository.add(newVersion);

    }
}
