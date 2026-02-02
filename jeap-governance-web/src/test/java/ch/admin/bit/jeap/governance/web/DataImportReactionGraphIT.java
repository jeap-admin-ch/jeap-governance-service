package ch.admin.bit.jeap.governance.web;

import ch.admin.bit.jeap.governance.archrepo.connector.model.ReactionGraphDto;
import ch.admin.bit.jeap.governance.archrepo.domain.ReactionGraph;
import ch.admin.bit.jeap.governance.archrepo.persistence.JpaReactionGraphRepository;
import ch.admin.bit.jeap.governance.dataimport.DataImportScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.time.ZonedDateTime;
import java.util.List;

import static ch.admin.bit.jeap.governance.web.ImportModelHelper.COMPONENT_A1_NAME;
import static ch.admin.bit.jeap.governance.web.ImportModelHelper.COMPONENT_A2_NAME;
import static ch.admin.bit.jeap.governance.web.ImportModelHelper.COMPONENT_A3_NAME;
import static ch.admin.bit.jeap.governance.web.ImportModelHelper.COMPONENT_B1_NAME;
import static ch.admin.bit.jeap.governance.web.ImportModelHelper.COMPONENT_C1_NAME;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DataImportReactionGraphIT extends ArchRepoMockIntegrationTestBase {

    @Autowired
    private JpaReactionGraphRepository repository;

    @Autowired
    private DataImportScheduler dataImportScheduler;

    @Test
    void synchronizeArchRepoModel_shouldImportReactionGraph_initial() throws Exception {
        setUpImportDefaultModel();
        List<ReactionGraphDto> reactionGraphDtos = List.of(
                new ReactionGraphDto(COMPONENT_A1_NAME, ZonedDateTime.now()),
                new ReactionGraphDto(COMPONENT_A2_NAME, ZonedDateTime.now()),
                new ReactionGraphDto(COMPONENT_A3_NAME, ZonedDateTime.now()),
                new ReactionGraphDto(COMPONENT_B1_NAME, ZonedDateTime.now()),
                new ReactionGraphDto(COMPONENT_C1_NAME, ZonedDateTime.now())
        );
        stubReactionGraphs(reactionGraphDtos);

        dataImportScheduler.update();

        Iterable<ReactionGraph> all = repository.findAll();
        assertThat(all).hasSize(5);
    }

    @Test
    void synchronizeArchRepoModel_shouldImportSystemsAndComponents_deletedSystemsAndComponents() throws Exception {
        setUpImportDefaultModel();
        List<ReactionGraphDto> reactionGraphDtos = List.of(
                new ReactionGraphDto(COMPONENT_A1_NAME, ZonedDateTime.now()),
                new ReactionGraphDto(COMPONENT_A2_NAME, ZonedDateTime.now()),
                new ReactionGraphDto(COMPONENT_A3_NAME, ZonedDateTime.now()), // System component A3 will be deleted later
                new ReactionGraphDto(COMPONENT_B1_NAME, ZonedDateTime.now()),
                new ReactionGraphDto(COMPONENT_C1_NAME, ZonedDateTime.now()) // System C will be deleted later
        );
        stubReactionGraphs(reactionGraphDtos);

        dataImportScheduler.update();

        Iterable<ReactionGraph> all = repository.findAll();
        assertThat(all).hasSize(5);

        setUpImportModelLess();

        dataImportScheduler.update();

        Iterable<ReactionGraph> allAfterDeletion = repository.findAll();
        assertThat(allAfterDeletion).hasSize(3);
    }

    @Test
    void synchronizeArchRepoModel_shouldImportSystemsAndComponents_addedSystemsAndComponents() throws Exception {
        setUpImportModelLess();
        // This should not happen in real life, the archrepo model should always be consistent with the relations, but
        // we test it anyways to ensure that the import can handle it and adds the missing systems and components
        List<ReactionGraphDto> reactionGraphDtos = List.of(
                new ReactionGraphDto(COMPONENT_A1_NAME, ZonedDateTime.now()),
                new ReactionGraphDto(COMPONENT_A2_NAME, ZonedDateTime.now()),
                new ReactionGraphDto(COMPONENT_A3_NAME, ZonedDateTime.now()), // System component A3 will be added later
                new ReactionGraphDto(COMPONENT_B1_NAME, ZonedDateTime.now()),
                new ReactionGraphDto(COMPONENT_C1_NAME, ZonedDateTime.now()) // System C will be added later
        );
        stubReactionGraphs(reactionGraphDtos);

        dataImportScheduler.update();

        Iterable<ReactionGraph> all = repository.findAll();
        assertThat(all).hasSize(3);

        setUpImportDefaultModel();

        dataImportScheduler.update();

        Iterable<ReactionGraph> allAfterDeletion = repository.findAll();
        assertThat(allAfterDeletion).hasSize(5);
    }
}
