package ch.admin.bit.jeap.governance.deploymentlog.synchronize;

import ch.admin.bit.jeap.governance.deploymentlog.connector.model.DeploymentLogComponentVersionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeploymentLogComponentVersionSynchronizer {

    private final DeploymentLogComponentVersionOneByOneSynchronizer oneByOneSynchronizer;

    public void synchronizeModelWithDeploymentLog(Set<DeploymentLogComponentVersionDto> dtos) {
        boolean hasException = false;
        for (DeploymentLogComponentVersionDto dto : dtos) {
            try {
                oneByOneSynchronizer.synchronize(dto);
            } catch (Exception e) {
                // Log and continue with next system to avoid blocking the whole synchronization in case of errors
                log.error("Error synchronizing DeploymentLogComponentVersionDto for system component {}: {}. Proceeding import", dto.componentName(), e.getMessage(), e);
                hasException = true;
            }
        }
        if (hasException) {
            throw new DeploymentLogSynchronizeException("Errors occurred during DeploymentLogComponentVersionDto synchronization. Check logs for details.");
        }
    }

}
