package ch.admin.bit.jeap.governance.reactionobserver.persistence;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.reactionobserver.domain.ReactionObserverComponentLastObservationDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ReactionObserverComponentLastObservationDateRepositoryImpl.class)
class ReactionObserverComponentLastObservationDateRepositoryImplTest extends PostgresTestContainerBase  {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ReactionObserverComponentLastObservationDateRepositoryImpl repository;

    @Test
    void add() {
        SystemComponent systemComponent = PersistenceTestUtility.createAndPersistSystemWithOneSystemComponent(entityManager);

        LocalDate observationDate = LocalDate.now();

        ReactionObserverComponentLastObservationDate lastObservationDate = ReactionObserverComponentLastObservationDate.builder()
                .systemComponent(systemComponent)
                .lastObservationDate(observationDate)
                .createdAt(ZonedDateTime.now())
                .build();

        repository.add(lastObservationDate);
        flushAndClear();

        ReactionObserverComponentLastObservationDate found = entityManager.find(ReactionObserverComponentLastObservationDate.class, lastObservationDate.getId());
        assertThat(found).isNotNull();
        assertThat(found.getId()).isNotNull();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getLastObservationDate()).isEqualTo(observationDate);
        assertThat(found.getSystemComponent().getId()).isEqualTo(systemComponent.getId());
    }

    @Test
    void findByComponentId() {
        LocalDate observationDate = LocalDate.now();
        SystemComponent systemComponent = PersistenceTestUtility.createAndPersistSystemWithOneSystemComponent(entityManager);
        ReactionObserverComponentLastObservationDate lastObservationDate = ReactionObserverComponentLastObservationDate.builder()
                .systemComponent(systemComponent)
                .lastObservationDate(observationDate)
                .build();

        repository.add(lastObservationDate);
        flushAndClear();

        Optional<ReactionObserverComponentLastObservationDate> found = repository.findByComponentId(systemComponent.getId());
        assertThat(found)
                .isNotNull()
                .isPresent();
    }

    @Test
    void findByComponentId_NotExisting() {
        SystemComponent systemComponent = PersistenceTestUtility.createAndPersistSystemWithOneSystemComponent(entityManager);
        flushAndClear();

        Optional<ReactionObserverComponentLastObservationDate> found = repository.findByComponentId(systemComponent.getId());
        assertThat(found)
                .isNotNull()
                .isEmpty();
    }

    @Test
    void findByComponentName() {
        LocalDate observationDate = LocalDate.now();
        SystemComponent systemComponent = PersistenceTestUtility.createAndPersistSystemWithOneSystemComponent(entityManager);
        ReactionObserverComponentLastObservationDate lastObservationDate = ReactionObserverComponentLastObservationDate.builder()
                .systemComponent(systemComponent)
                .lastObservationDate(observationDate)
                .build();
        repository.add(lastObservationDate);
        flushAndClear();

        Optional<ReactionObserverComponentLastObservationDate> found = repository.findByComponentName(systemComponent.getName());
        assertThat(found)
                .isNotNull()
                .isPresent();
    }

    @Test
    void findByComponentName_NotExisting() {
        SystemComponent systemComponent = PersistenceTestUtility.createAndPersistSystemWithOneSystemComponent(entityManager);
        flushAndClear();

        Optional<ReactionObserverComponentLastObservationDate> found = repository.findByComponentName(systemComponent.getName());
        assertThat(found)
                .isNotNull()
                .isEmpty();
    }

    @Test
    void delete() {
        LocalDate observationDate = LocalDate.now();
        SystemComponent systemComponent = PersistenceTestUtility.createAndPersistSystemWithOneSystemComponent(entityManager);
        ReactionObserverComponentLastObservationDate lastObservationDate = ReactionObserverComponentLastObservationDate.builder()
                .systemComponent(systemComponent)
                .lastObservationDate(observationDate)
                .build();
        repository.add(lastObservationDate);
        flushAndClear();

        ReactionObserverComponentLastObservationDate found = entityManager.find(ReactionObserverComponentLastObservationDate.class, lastObservationDate.getId());
        assertThat(found).isNotNull();

        repository.delete(found);
        flushAndClear();

        ReactionObserverComponentLastObservationDate deleted = entityManager.find(ReactionObserverComponentLastObservationDate.class, lastObservationDate.getId());
        assertThat(deleted).isNull();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }


}
