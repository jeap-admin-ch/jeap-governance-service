package ch.admin.bit.jeap.governance.web;

import ch.admin.bit.jeap.governance.deploymentlog.DeploymentLogProperties;
import ch.admin.bit.jeap.governance.deploymentlog.dataimport.DeploymentLogComponentVersionImporter;
import ch.admin.bit.jeap.governance.deploymentlog.deletion.DeploymentLogComponentVersionDeletionListener;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "jeap.governance.environment=DEV",
        "jeap.governance.deploymentlog.enabled=false",
        "jeap.governance.archrepo.url=http://localhost:8081",
})
class DeploymentLogComponentVersionDisabledIT extends PostgresTestContainerBase {

    @Autowired
    private ApplicationContext context;

    @Test
    void deploymentLogComponentVersionBeans_shouldNotExist_whenDisabled() {
        assertThat(context.getBeansOfType(DeploymentLogComponentVersionImporter.class)).isEmpty();
        assertThat(context.getBeansOfType(DeploymentLogComponentVersionDeletionListener.class)).isEmpty();
        assertThat(context.getBeansOfType(DeploymentLogProperties.class)).isEmpty();
    }
}
