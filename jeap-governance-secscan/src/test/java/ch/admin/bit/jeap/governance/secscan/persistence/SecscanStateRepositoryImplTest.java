package ch.admin.bit.jeap.governance.secscan.persistence;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.secscan.domain.SecscanState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(SecscanStateRepositoryImpl.class)
class SecscanStateRepositoryImplTest extends PostgresTestContainerBase {

    private static final String COMPONENT_A = "testa-backend-svc";
    private static final String COMPONENT_B = "testb-api-svc";

    @Autowired
    private SecscanStateRepositoryImpl repository;

    @Test
    void save() {
        SystemComponent componentA = persistSystemComponent(COMPONENT_A);
        ZonedDateTime scanTimestamp = ZonedDateTime.now().truncatedTo(ChronoUnit.MICROS);
        SecscanState state = SecscanState.builder()
                .systemComponentId(componentA.getId())
                .scanMessage("All endpoints secured")
                .scanTimestamp(scanTimestamp)
                .build();

        repository.save(state);
        flushAndClear();

        SecscanState found = entityManager.find(SecscanState.class, state.getId());
        assertThat(found).isNotNull();
        assertThat(found.getSystemComponentId()).isEqualTo(componentA.getId());
        assertThat(found.getScanMessage()).isEqualTo("All endpoints secured");
        assertThat(found.getScanTimestamp().toInstant()).isEqualTo(scanTimestamp.toInstant());
    }

    @Test
    void save_withNullMessage() {
        SystemComponent componentA = persistSystemComponent(COMPONENT_A);
        ZonedDateTime scanTimestamp = ZonedDateTime.now().truncatedTo(ChronoUnit.MICROS);
        SecscanState state = SecscanState.builder()
                .systemComponentId(componentA.getId())
                .scanTimestamp(scanTimestamp)
                .build();

        repository.save(state);
        flushAndClear();

        SecscanState found = entityManager.find(SecscanState.class, state.getId());
        assertThat(found).isNotNull();
        assertThat(found.getSystemComponentId()).isEqualTo(componentA.getId());
        assertThat(found.getScanMessage()).isNull();
        assertThat(found.getScanTimestamp().toInstant()).isEqualTo(scanTimestamp.toInstant());
    }

    @Test
    void findBySystemComponentId() {
        SystemComponent componentA = persistSystemComponent(COMPONENT_A);
        SystemComponent componentB = persistSystemComponent(COMPONENT_B);
        ZonedDateTime scanTimestamp = ZonedDateTime.now().truncatedTo(ChronoUnit.MICROS);
        SecscanState stateA = SecscanState.builder()
                .systemComponentId(componentA.getId())
                .scanMessage("2 flagged endpoints")
                .scanTimestamp(scanTimestamp)
                .build();
        SecscanState stateB = SecscanState.builder()
                .systemComponentId(componentB.getId())
                .scanMessage("OK")
                .scanTimestamp(scanTimestamp)
                .build();
        entityManager.persist(stateA);
        entityManager.persist(stateB);
        flushAndClear();

        Optional<SecscanState> result = repository.findBySystemComponentId(componentA.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getSystemComponentId()).isEqualTo(componentA.getId());
        assertThat(result.get().getScanMessage()).isEqualTo("2 flagged endpoints");
        assertThat(result.get().getScanTimestamp().toInstant()).isEqualTo(scanTimestamp.toInstant());
    }

    @Test
    void findBySystemComponentId_noMatch() {
        SystemComponent componentA = persistSystemComponent(COMPONENT_A);
        SystemComponent componentB = persistSystemComponent(COMPONENT_B);
        SecscanState stateB = SecscanState.builder()
                .systemComponentId(componentB.getId())
                .scanMessage("OK")
                .scanTimestamp(ZonedDateTime.now())
                .build();
        entityManager.persist(stateB);
        flushAndClear();

        Optional<SecscanState> result = repository.findBySystemComponentId(componentA.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void deleteBySystemComponentId() {
        SystemComponent componentA = persistSystemComponent(COMPONENT_A);
        SystemComponent componentB = persistSystemComponent(COMPONENT_B);
        SecscanState stateA = SecscanState.builder()
                .systemComponentId(componentA.getId())
                .scanMessage("flagged")
                .scanTimestamp(ZonedDateTime.now())
                .build();
        SecscanState stateB = SecscanState.builder()
                .systemComponentId(componentB.getId())
                .scanMessage("OK")
                .scanTimestamp(ZonedDateTime.now())
                .build();
        entityManager.persist(stateA);
        entityManager.persist(stateB);
        flushAndClear();

        int deletedCount = repository.deleteBySystemComponentId(componentA.getId());

        assertThat(deletedCount).isEqualTo(1);
        assertThat(entityManager.find(SecscanState.class, stateA.getId())).isNull();
        assertThat(entityManager.find(SecscanState.class, stateB.getId())).isNotNull();
    }

    @Test
    void deleteBySystemComponentId_noMatch() {
        SystemComponent componentA = persistSystemComponent(COMPONENT_A);
        SystemComponent componentB = persistSystemComponent(COMPONENT_B);
        SecscanState stateB = SecscanState.builder()
                .systemComponentId(componentB.getId())
                .scanMessage("OK")
                .scanTimestamp(ZonedDateTime.now())
                .build();
        entityManager.persist(stateB);
        flushAndClear();

        int deletedCount = repository.deleteBySystemComponentId(componentA.getId());

        assertThat(deletedCount).isEqualTo(0);
        assertThat(entityManager.find(SecscanState.class, stateB.getId())).isNotNull();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
