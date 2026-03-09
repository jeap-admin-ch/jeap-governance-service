package ch.admin.bit.jeap.governance.web;

import ch.admin.bit.jeap.governance.dataimport.DataImportScheduler;
import ch.admin.bit.jeap.governance.reactionobserver.domain.ReactionObserverComponentLastObservationDate;
import ch.admin.bit.jeap.governance.reactionobserver.persistence.JpaReactionObserverComponentLastObservationDateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDate;
import java.util.Map;

import static ch.admin.bit.jeap.governance.web.ImportModelHelper.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"jeap.governance.reactionobserver.enabled=true"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DataImportReactionObserverComponentLastObservationDateIT extends GovernanceIntegrationTestBase {

    @Autowired
    private JpaReactionObserverComponentLastObservationDateRepository repository;

    @Autowired
    private DataImportScheduler dataImportScheduler;

    @Test
    void synchronizeArchRepoModel_shouldImportDeploymentLogComponentVersion_initial() throws Exception {
        setUpImportDefaultModel();
        Map<String, LocalDate> dates = Map.of(
                COMPONENT_A1_NAME, LocalDate.of(1998, 7, 14),
                COMPONENT_A2_NAME, LocalDate.of(1998, 7, 14),
                COMPONENT_A3_NAME, LocalDate.of(1998, 7, 14),
                COMPONENT_B1_NAME, LocalDate.of(1998, 7, 14),
                COMPONENT_C1_NAME, LocalDate.of(1998, 7, 14)
        );
        stubReactionObserverComponentLastObservationDates(dates);

        dataImportScheduler.update();

        Iterable<ReactionObserverComponentLastObservationDate> all = repository.findAll();
        assertThat(all).hasSize(5);
    }

    @Test
    void synchronizeArchRepoModel_shouldImportSystemsAndComponents_deletedSystemsAndComponents() throws Exception {
        setUpImportDefaultModel();
        Map<String, LocalDate> dates = Map.of(
                COMPONENT_A1_NAME, LocalDate.of(1998, 7, 14),
                COMPONENT_A2_NAME, LocalDate.of(1998, 7, 14),
                COMPONENT_A3_NAME, LocalDate.of(1998, 7, 14),
                COMPONENT_B1_NAME, LocalDate.of(1998, 7, 14),
                COMPONENT_C1_NAME, LocalDate.of(1998, 7, 14)
        );
        stubReactionObserverComponentLastObservationDates(dates);

        dataImportScheduler.update();

        Iterable<ReactionObserverComponentLastObservationDate> all = repository.findAll();
        assertThat(all).hasSize(5);

        setUpImportModelLess();

        dataImportScheduler.update();

        Iterable<ReactionObserverComponentLastObservationDate> allAfterDeletion = repository.findAll();
        assertThat(allAfterDeletion).hasSize(3);
    }

    @Test
    void synchronizeArchRepoModel_shouldImportSystemsAndComponents_addedSystemsAndComponents() throws Exception {
        setUpImportModelLess();
        // This should not happen in real life, the archrepo model should always be consistent with the relations, but
        // we test it anyways to ensure that the import can handle it and adds the missing systems and components
        Map<String, LocalDate> dates = Map.of(
                COMPONENT_A1_NAME, LocalDate.of(1998, 7, 14),
                COMPONENT_A2_NAME, LocalDate.of(1998, 7, 14),
                COMPONENT_A3_NAME, LocalDate.of(1998, 7, 14),
                COMPONENT_B1_NAME, LocalDate.of(1998, 7, 14),
                COMPONENT_C1_NAME, LocalDate.of(1998, 7, 14)
        );
        stubReactionObserverComponentLastObservationDates(dates);

        dataImportScheduler.update();

        Iterable<ReactionObserverComponentLastObservationDate> all = repository.findAll();
        assertThat(all).hasSize(3);

        setUpImportDefaultModel();

        dataImportScheduler.update();

        Iterable<ReactionObserverComponentLastObservationDate> allAfterDeletion = repository.findAll();
        assertThat(allAfterDeletion).hasSize(5);
    }
}
