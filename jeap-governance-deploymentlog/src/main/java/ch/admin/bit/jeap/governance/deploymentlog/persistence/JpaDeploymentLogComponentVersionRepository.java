package ch.admin.bit.jeap.governance.deploymentlog.persistence;

import ch.admin.bit.jeap.governance.deploymentlog.domain.DeploymentLogComponentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaDeploymentLogComponentVersionRepository extends JpaRepository<DeploymentLogComponentVersion, Long> {

    Optional<DeploymentLogComponentVersion> findBySystemComponentName(String componentName);

    Optional<DeploymentLogComponentVersion> findBySystemComponentId(long componentId);
}
