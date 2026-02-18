package ch.admin.bit.jeap.governance.deploymentlog.domain;

import java.util.Optional;

public interface DeploymentLogComponentVersionRepository {

    Optional<DeploymentLogComponentVersion> findByComponentId(long id);

    Optional<DeploymentLogComponentVersion> findByComponentName(String componentName);

    DeploymentLogComponentVersion add(DeploymentLogComponentVersion deploymentLogComponentVersion);

    void delete(DeploymentLogComponentVersion deploymentLogComponentVersion);

}
