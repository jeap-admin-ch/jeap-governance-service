package ch.admin.bit.jeap.governance.web;

import ch.admin.bit.jeap.governance.archrepo.connector.model.ApiDocVersionDto;
import ch.admin.bit.jeap.governance.archrepo.domain.ApiDocVersion;
import ch.admin.bit.jeap.governance.archrepo.persistence.JpaApiDocVersionRepository;
import ch.admin.bit.jeap.governance.dataimport.DataImportScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static ch.admin.bit.jeap.governance.web.ImportModelHelper.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DataImportApiDocVersionIT extends ArchRepoMockIntegrationTestBase {

    @Autowired
    private JpaApiDocVersionRepository repository;

    @Autowired
    private DataImportScheduler dataImportScheduler;

    @Test
    void synchronizeArchRepoModel_shouldImportApiDocVersion_initial() throws Exception {
        setUpImportDefaultModel();
        List<ApiDocVersionDto> apiDocVersionDtos = List.of(
                new ApiDocVersionDto(SYSTEM_A_NAME, COMPONENT_A1_NAME, "0.0.1"),
                new ApiDocVersionDto(SYSTEM_A_NAME, COMPONENT_A2_NAME, "0.0.1"),
                new ApiDocVersionDto(SYSTEM_A_NAME, COMPONENT_A3_NAME, "0.0.1"),
                new ApiDocVersionDto(SYSTEM_B_NAME, COMPONENT_B1_NAME, "0.0.1"),
                new ApiDocVersionDto(SYSTEM_C_NAME, COMPONENT_C1_NAME, "0.0.1")
        );
        stubApiDocVersions(apiDocVersionDtos);

        dataImportScheduler.update();

        Iterable<ApiDocVersion> all = repository.findAll();
        assertThat(all).hasSize(5);
    }

    @Test
    void synchronizeArchRepoModel_shouldImportSystemsAndComponents_deletedSystemsAndComponents() throws Exception {
        setUpImportDefaultModel();
        List<ApiDocVersionDto> apiDocVersionDtos = List.of(
                new ApiDocVersionDto(SYSTEM_A_NAME, COMPONENT_A1_NAME, "0.0.1"),
                new ApiDocVersionDto(SYSTEM_A_NAME, COMPONENT_A2_NAME, "0.0.1"),
                new ApiDocVersionDto(SYSTEM_A_NAME, COMPONENT_A3_NAME, "0.0.1"), // System component A3 will be deleted later
                new ApiDocVersionDto(SYSTEM_B_NAME, COMPONENT_B1_NAME, "0.0.1"),
                new ApiDocVersionDto(SYSTEM_C_NAME, COMPONENT_C1_NAME, "0.0.1") // System C will be deleted later
        );
        stubApiDocVersions(apiDocVersionDtos);

        dataImportScheduler.update();

        Iterable<ApiDocVersion> all = repository.findAll();
        assertThat(all).hasSize(5);

        setUpImportModelLess();

        dataImportScheduler.update();

        Iterable<ApiDocVersion> allAfterDeletion = repository.findAll();
        assertThat(allAfterDeletion).hasSize(3);
    }

    @Test
    void synchronizeArchRepoModel_shouldImportSystemsAndComponents_addedSystemsAndComponents() throws Exception {
        setUpImportModelLess();
        // This should not happen in real life, the archrepo model should always be consistent with the relations, but
        // we test it anyways to ensure that the import can handle it and adds the missing systems and components
        List<ApiDocVersionDto> apiDocVersionDtos = List.of(
                new ApiDocVersionDto(SYSTEM_A_NAME, COMPONENT_A1_NAME, "0.0.1"),
                new ApiDocVersionDto(SYSTEM_A_NAME, COMPONENT_A2_NAME, "0.0.1"),
                new ApiDocVersionDto(SYSTEM_A_NAME, COMPONENT_A3_NAME, "0.0.1"), // System component A3 will be added later
                new ApiDocVersionDto(SYSTEM_B_NAME, COMPONENT_B1_NAME, "0.0.1"),
                new ApiDocVersionDto(SYSTEM_C_NAME, COMPONENT_C1_NAME, "0.0.1") // System C will be added later
        );
        stubApiDocVersions(apiDocVersionDtos);

        dataImportScheduler.update();

        Iterable<ApiDocVersion> all = repository.findAll();
        assertThat(all).hasSize(3);

        setUpImportDefaultModel();

        dataImportScheduler.update();

        Iterable<ApiDocVersion> allAfterDeletion = repository.findAll();
        assertThat(allAfterDeletion).hasSize(5);
    }
}
