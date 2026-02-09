package ch.admin.bit.jeap.governance.deploymentlog.persistence;

import ch.admin.bit.jeap.governance.deploymentlog.domain.DeploymentLogComponentVersion;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface JpaDeploymentLogComponentVersionRepository extends CrudRepository<DeploymentLogComponentVersion, Long> {

    Optional<DeploymentLogComponentVersion> findBySystemComponentName(String componentName);

    Optional<DeploymentLogComponentVersion> findBySystemComponentId(Long componentId);
}
