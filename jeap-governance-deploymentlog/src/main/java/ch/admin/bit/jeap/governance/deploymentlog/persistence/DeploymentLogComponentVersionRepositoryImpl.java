package ch.admin.bit.jeap.governance.deploymentlog.persistence;

import ch.admin.bit.jeap.governance.deploymentlog.domain.DeploymentLogComponentVersion;
import ch.admin.bit.jeap.governance.deploymentlog.domain.DeploymentLogComponentVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DeploymentLogComponentVersionRepositoryImpl implements DeploymentLogComponentVersionRepository {

    private final JpaDeploymentLogComponentVersionRepository jpaRepository;

    @Override
    public Optional<DeploymentLogComponentVersion> findByComponentId(Long componentId) {
        return jpaRepository.findBySystemComponentId(componentId);
    }

    @Override
    public Optional<DeploymentLogComponentVersion> findByComponentName(String componentName) {
        return jpaRepository.findBySystemComponentName(componentName);
    }

    @Override
    public DeploymentLogComponentVersion add(DeploymentLogComponentVersion deploymentLogComponentVersion) {
        return jpaRepository.save(deploymentLogComponentVersion);
    }

    @Override
    public void delete(DeploymentLogComponentVersion deploymentLogComponentVersion) {
        jpaRepository.delete(deploymentLogComponentVersion);
    }
}
