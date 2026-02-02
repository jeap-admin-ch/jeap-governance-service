package ch.admin.bit.jeap.governance.web;

import ch.admin.bit.jeap.governance.archrepo.connector.model.RestApiRelationWithoutPactDto;
import ch.admin.bit.jeap.governance.archrepo.domain.RestApiRelationWithoutPact;
import ch.admin.bit.jeap.governance.archrepo.persistence.JpaRestApiRelationWithoutPactRepository;
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
class DataImportRestApiRelationWithoutPactIT extends ArchRepoMockIntegrationTestBase {

    @Autowired
    private JpaRestApiRelationWithoutPactRepository repository;

    @Autowired
    private DataImportScheduler dataImportScheduler;

    @Test
    void synchronizeArchRepoModel_shouldImportRestApiRelationWithoutPact_initial() throws Exception {
        setUpImportDefaultModel();
        List<RestApiRelationWithoutPactDto> restApiRelationsWithoutPact = List.of(
                new RestApiRelationWithoutPactDto(SYSTEM_A_NAME, COMPONENT_A1_NAME, SYSTEM_A_NAME, COMPONENT_A2_NAME, "GET", "/api/resource1"),
                new RestApiRelationWithoutPactDto(SYSTEM_A_NAME, COMPONENT_A1_NAME, SYSTEM_B_NAME, COMPONENT_B1_NAME, "GET", "/api/resource1"),
                new RestApiRelationWithoutPactDto(SYSTEM_A_NAME, COMPONENT_A1_NAME, SYSTEM_B_NAME, COMPONENT_B3_NAME, "GET", "/api/resource1"),
                new RestApiRelationWithoutPactDto(SYSTEM_A_NAME, COMPONENT_A1_NAME, SYSTEM_C_NAME, COMPONENT_C1_NAME, "GET", "/api/resource1"),
                new RestApiRelationWithoutPactDto(SYSTEM_C_NAME, COMPONENT_C1_NAME, SYSTEM_B_NAME, COMPONENT_B1_NAME, "GET", "/api/resource1"),
                new RestApiRelationWithoutPactDto(SYSTEM_C_NAME, COMPONENT_C1_NAME, SYSTEM_C_NAME, COMPONENT_C2_NAME, "GET", "/api/resource1")
        );
        stubRestApiRelationsWithoutPact(restApiRelationsWithoutPact);

        dataImportScheduler.update();

        Iterable<RestApiRelationWithoutPact> all = repository.findAll();
        assertThat(all).hasSize(6);
    }

    @Test
    void synchronizeArchRepoModel_shouldRestApiRelationWithoutPact_deletedSystemsAndComponents() throws Exception {
        setUpImportDefaultModel();
        List<RestApiRelationWithoutPactDto> restApiRelationsWithoutPact = List.of(
                new RestApiRelationWithoutPactDto(SYSTEM_A_NAME, COMPONENT_A1_NAME, SYSTEM_A_NAME, COMPONENT_A2_NAME, "GET", "/api/resource1"),
                new RestApiRelationWithoutPactDto(SYSTEM_A_NAME, COMPONENT_A1_NAME, SYSTEM_B_NAME, COMPONENT_B1_NAME, "GET", "/api/resource1"),
                new RestApiRelationWithoutPactDto(SYSTEM_A_NAME, COMPONENT_A1_NAME, SYSTEM_B_NAME, COMPONENT_B3_NAME, "GET", "/api/resource1"), // System component A3 will be deleted later
                new RestApiRelationWithoutPactDto(SYSTEM_A_NAME, COMPONENT_A1_NAME, SYSTEM_C_NAME, COMPONENT_C1_NAME, "GET", "/api/resource1"), // System C will be deleted later
                new RestApiRelationWithoutPactDto(SYSTEM_C_NAME, COMPONENT_C1_NAME, SYSTEM_B_NAME, COMPONENT_B1_NAME, "GET", "/api/resource1"), // System C will be deleted later
                new RestApiRelationWithoutPactDto(SYSTEM_C_NAME, COMPONENT_C1_NAME, SYSTEM_C_NAME, COMPONENT_C2_NAME, "GET", "/api/resource1") // System C will be deleted later
        );
        stubRestApiRelationsWithoutPact(restApiRelationsWithoutPact);

        dataImportScheduler.update();

        Iterable<RestApiRelationWithoutPact> all = repository.findAll();
        assertThat(all).hasSize(6);

        setUpImportModelLess();

        dataImportScheduler.update();

        Iterable<RestApiRelationWithoutPact> allAfterDeletion = repository.findAll();
        assertThat(allAfterDeletion).hasSize(2);
    }

    @Test
    void synchronizeArchRepoModel_shouldRestApiRelationWithoutPact_addedSystemsAndComponents() throws Exception {
        setUpImportModelLess();
        // This should not happen in real life, the archrepo model should always be consistent with the relations, but
        // we test it anyways to ensure that the import can handle it and adds the missing systems and components
        List<RestApiRelationWithoutPactDto> restApiRelationsWithoutPact = List.of(
                new RestApiRelationWithoutPactDto(SYSTEM_A_NAME, COMPONENT_A1_NAME, SYSTEM_A_NAME, COMPONENT_A2_NAME, "GET", "/api/resource1"),
                new RestApiRelationWithoutPactDto(SYSTEM_A_NAME, COMPONENT_A1_NAME, SYSTEM_B_NAME, COMPONENT_B1_NAME, "GET", "/api/resource1"),
                new RestApiRelationWithoutPactDto(SYSTEM_A_NAME, COMPONENT_A1_NAME, SYSTEM_B_NAME, COMPONENT_B3_NAME, "GET", "/api/resource1"), // System component A3 will be added later
                new RestApiRelationWithoutPactDto(SYSTEM_A_NAME, COMPONENT_A1_NAME, SYSTEM_C_NAME, COMPONENT_C1_NAME, "GET", "/api/resource1"), // System C will be added later
                new RestApiRelationWithoutPactDto(SYSTEM_C_NAME, COMPONENT_C1_NAME, SYSTEM_B_NAME, COMPONENT_B1_NAME, "GET", "/api/resource1"), // System C will be added later
                new RestApiRelationWithoutPactDto(SYSTEM_C_NAME, COMPONENT_C1_NAME, SYSTEM_C_NAME, COMPONENT_C2_NAME, "GET", "/api/resource1")
        );
        stubRestApiRelationsWithoutPact(restApiRelationsWithoutPact);

        dataImportScheduler.update();

        Iterable<RestApiRelationWithoutPact> all = repository.findAll();
        assertThat(all).hasSize(2);

        setUpImportDefaultModel();

        dataImportScheduler.update();

        Iterable<RestApiRelationWithoutPact> allAfterDeletion = repository.findAll();
        assertThat(allAfterDeletion).hasSize(6);
    }
}
