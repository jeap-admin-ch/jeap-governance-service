package ch.admin.bit.jeap.governance.web;

import ch.admin.bit.jeap.governance.dataimport.DataImportScheduler;
import ch.admin.bit.jeap.governance.deploymentlog.connector.model.DeploymentLogComponentVersionDto;
import ch.admin.bit.jeap.governance.deploymentlog.domain.DeploymentLogComponentVersion;
import ch.admin.bit.jeap.governance.deploymentlog.persistence.JpaDeploymentLogComponentVersionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Set;

import static ch.admin.bit.jeap.governance.web.ImportModelHelper.COMPONENT_A1_NAME;
import static ch.admin.bit.jeap.governance.web.ImportModelHelper.COMPONENT_A2_NAME;
import static ch.admin.bit.jeap.governance.web.ImportModelHelper.COMPONENT_A3_NAME;
import static ch.admin.bit.jeap.governance.web.ImportModelHelper.COMPONENT_B1_NAME;
import static ch.admin.bit.jeap.governance.web.ImportModelHelper.COMPONENT_C1_NAME;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DataImportDeploymentLogComponentVersionIT extends GovernanceIntegrationTestBase {

    @Autowired
    private JpaDeploymentLogComponentVersionRepository repository;

    @Autowired
    private DataImportScheduler dataImportScheduler;

    @Test
    void synchronizeArchRepoModel_shouldImportDeploymentLogComponentVersion_initial() throws Exception {
        setUpImportDefaultModel();
        Set<DeploymentLogComponentVersionDto> dtos = Set.of(
                new DeploymentLogComponentVersionDto(COMPONENT_A1_NAME, "0.0.1"),
                new DeploymentLogComponentVersionDto(COMPONENT_A2_NAME, "0.0.2"),
                new DeploymentLogComponentVersionDto(COMPONENT_A3_NAME, "0.1.1"),
                new DeploymentLogComponentVersionDto(COMPONENT_B1_NAME, "0.2.1"),
                new DeploymentLogComponentVersionDto(COMPONENT_C1_NAME, "1.0.1")
        );
        stubDeploymentLogDeploymentLogComponentVersions(dtos);

        dataImportScheduler.update();

        Iterable<DeploymentLogComponentVersion> all = repository.findAll();
        assertThat(all).hasSize(5);
    }

    @Test
    void synchronizeArchRepoModel_shouldImportSystemsAndComponents_deletedSystemsAndComponents() throws Exception {
        setUpImportDefaultModel();
        Set<DeploymentLogComponentVersionDto> dtos = Set.of(
                new DeploymentLogComponentVersionDto(COMPONENT_A1_NAME, "0.0.1"),
                new DeploymentLogComponentVersionDto(COMPONENT_A2_NAME, "0.0.2"),
                new DeploymentLogComponentVersionDto(COMPONENT_A3_NAME, "0.1.1"),
                new DeploymentLogComponentVersionDto(COMPONENT_B1_NAME, "0.2.1"),
                new DeploymentLogComponentVersionDto(COMPONENT_C1_NAME, "1.0.1")
        );
        stubDeploymentLogDeploymentLogComponentVersions(dtos);

        dataImportScheduler.update();

        Iterable<DeploymentLogComponentVersion> all = repository.findAll();
        assertThat(all).hasSize(5);

        setUpImportModelLess();

        dataImportScheduler.update();

        Iterable<DeploymentLogComponentVersion> allAfterDeletion = repository.findAll();
        assertThat(allAfterDeletion).hasSize(3);
    }

    @Test
    void synchronizeArchRepoModel_shouldImportSystemsAndComponents_addedSystemsAndComponents() throws Exception {
        setUpImportModelLess();
        // This should not happen in real life, the archrepo model should always be consistent with the relations, but
        // we test it anyways to ensure that the import can handle it and adds the missing systems and components
        Set<DeploymentLogComponentVersionDto> dtos = Set.of(
                new DeploymentLogComponentVersionDto(COMPONENT_A1_NAME, "0.0.1"),
                new DeploymentLogComponentVersionDto(COMPONENT_A2_NAME, "0.0.1"),
                new DeploymentLogComponentVersionDto(COMPONENT_A3_NAME, "0.0.1"), // System component A3 will be added later
                new DeploymentLogComponentVersionDto(COMPONENT_B1_NAME, "0.0.1"),
                new DeploymentLogComponentVersionDto(COMPONENT_C1_NAME, "0.0.1") // System C will be added later
        );
        stubDeploymentLogDeploymentLogComponentVersions(dtos);

        dataImportScheduler.update();

        Iterable<DeploymentLogComponentVersion> all = repository.findAll();
        assertThat(all).hasSize(3);

        setUpImportDefaultModel();

        dataImportScheduler.update();

        Iterable<DeploymentLogComponentVersion> allAfterDeletion = repository.findAll();
        assertThat(allAfterDeletion).hasSize(5);
    }
}
