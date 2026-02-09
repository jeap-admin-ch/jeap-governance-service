package ch.admin.bit.jeap.governance.web;

import ch.admin.bit.jeap.governance.archrepo.connector.model.DatabaseSchemaVersionDto;
import ch.admin.bit.jeap.governance.archrepo.domain.DatabaseSchemaVersion;
import ch.admin.bit.jeap.governance.archrepo.persistence.JpaDatabaseSchemaVersionRepository;
import ch.admin.bit.jeap.governance.dataimport.DataImportScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static ch.admin.bit.jeap.governance.web.ImportModelHelper.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DataImportDatabaseSchemaVersionIT extends GovernanceIntegrationTestBase {

    @Autowired
    private JpaDatabaseSchemaVersionRepository repository;

    @Autowired
    private DataImportScheduler dataImportScheduler;

    @Test
    void synchronizeArchRepoModel_shouldImportDatabaseSchemaVersion_initial() throws Exception {
        setUpImportDefaultModel();
        List<DatabaseSchemaVersionDto> databaseSchemaVersionDtos = List.of(
                new DatabaseSchemaVersionDto(SYSTEM_A_NAME, COMPONENT_A1_NAME, "0.0.1"),
                new DatabaseSchemaVersionDto(SYSTEM_A_NAME, COMPONENT_A2_NAME, "0.0.1"),
                new DatabaseSchemaVersionDto(SYSTEM_A_NAME, COMPONENT_A3_NAME, "0.0.1"),
                new DatabaseSchemaVersionDto(SYSTEM_B_NAME, COMPONENT_B1_NAME, "0.0.1"),
                new DatabaseSchemaVersionDto(SYSTEM_C_NAME, COMPONENT_C1_NAME, "0.0.1")
        );
        stubArchRepoDatabaseSchemaVersions(databaseSchemaVersionDtos);

        dataImportScheduler.update();

        Iterable<DatabaseSchemaVersion> all = repository.findAll();
        assertThat(all).hasSize(5);
    }

    @Test
    void synchronizeArchRepoModel_shouldImportSystemsAndComponents_deletedSystemsAndComponents() throws Exception {
        setUpImportDefaultModel();
        List<DatabaseSchemaVersionDto> databaseSchemaVersionDtos = List.of(
                new DatabaseSchemaVersionDto(SYSTEM_A_NAME, COMPONENT_A1_NAME, "0.0.1"),
                new DatabaseSchemaVersionDto(SYSTEM_A_NAME, COMPONENT_A2_NAME, "0.0.1"),
                new DatabaseSchemaVersionDto(SYSTEM_A_NAME, COMPONENT_A3_NAME, "0.0.1"), // System component A3 will be deleted later
                new DatabaseSchemaVersionDto(SYSTEM_B_NAME, COMPONENT_B1_NAME, "0.0.1"),
                new DatabaseSchemaVersionDto(SYSTEM_C_NAME, COMPONENT_C1_NAME, "0.0.1") // System C will be deleted later
        );
        stubArchRepoDatabaseSchemaVersions(databaseSchemaVersionDtos);

        dataImportScheduler.update();

        Iterable<DatabaseSchemaVersion> all = repository.findAll();
        assertThat(all).hasSize(5);

        setUpImportModelLess();

        dataImportScheduler.update();

        Iterable<DatabaseSchemaVersion> allAfterDeletion = repository.findAll();
        assertThat(allAfterDeletion).hasSize(3);
    }

    @Test
    void synchronizeArchRepoModel_shouldImportSystemsAndComponents_addedSystemsAndComponents() throws Exception {
        setUpImportModelLess();
        // This should not happen in real life, the archrepo model should always be consistent with the relations, but
        // we test it anyways to ensure that the import can handle it and adds the missing systems and components
        List<DatabaseSchemaVersionDto> databaseSchemaVersionDtos = List.of(
                new DatabaseSchemaVersionDto(SYSTEM_A_NAME, COMPONENT_A1_NAME, "0.0.1"),
                new DatabaseSchemaVersionDto(SYSTEM_A_NAME, COMPONENT_A2_NAME, "0.0.1"),
                new DatabaseSchemaVersionDto(SYSTEM_A_NAME, COMPONENT_A3_NAME, "0.0.1"), // System component A3 will be added later
                new DatabaseSchemaVersionDto(SYSTEM_B_NAME, COMPONENT_B1_NAME, "0.0.1"),
                new DatabaseSchemaVersionDto(SYSTEM_C_NAME, COMPONENT_C1_NAME, "0.0.1") // System C will be added later
        );
        stubArchRepoDatabaseSchemaVersions(databaseSchemaVersionDtos);

        dataImportScheduler.update();

        Iterable<DatabaseSchemaVersion> all = repository.findAll();
        assertThat(all).hasSize(3);

        setUpImportDefaultModel();

        dataImportScheduler.update();

        Iterable<DatabaseSchemaVersion> allAfterDeletion = repository.findAll();
        assertThat(allAfterDeletion).hasSize(5);
    }
}
