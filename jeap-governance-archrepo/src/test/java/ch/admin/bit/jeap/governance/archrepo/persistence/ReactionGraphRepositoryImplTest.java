package ch.admin.bit.jeap.governance.archrepo.persistence;

import ch.admin.bit.jeap.governance.archrepo.domain.ReactionGraph;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ReactionGraphRepositoryImpl.class)
class ReactionGraphRepositoryImplTest extends PostgresTestContainerBase {

    private static final ZonedDateTime LAST_MODIFIED_AT = ZonedDateTime.now().minusDays(1);
    private static final ZonedDateTime LAST_MODIFIED_AT_NEWER = ZonedDateTime.now();

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ReactionGraphRepositoryImpl repository;

    @Test
    void add() {
        SystemComponent systemComponent = PersistenceTestUtility.createAndPersistSystemWithOneSystemComponent(entityManager);

        ReactionGraph reactionGraph = ReactionGraph.builder()
                .id(UUID.randomUUID())
                .systemComponent(systemComponent)
                .lastModifiedAt(LAST_MODIFIED_AT)
                .build();

        repository.add(reactionGraph);
        flushAndClear();

        ReactionGraph found = entityManager.find(ReactionGraph.class, reactionGraph.getId());
        assertNotNull(found);
        assertNotNull(found.getId());
        assertNotNull(found.getCreatedAt());
        assertThat(found.getLastModifiedAt())
                .isCloseTo(LAST_MODIFIED_AT, within(1, ChronoUnit.MILLIS));
        assertEquals(systemComponent.getId(), found.getSystemComponent().getId());
    }

    @Test
    void update() {
        SystemComponent systemComponent = PersistenceTestUtility.createAndPersistSystemWithOneSystemComponent(entityManager);
        ReactionGraph reactionGraph = ReactionGraph.builder()
                .id(UUID.randomUUID())
                .systemComponent(systemComponent)
                .lastModifiedAt(LAST_MODIFIED_AT)
                .build();
        repository.add(reactionGraph);
        flushAndClear();

        ReactionGraph found = entityManager.find(ReactionGraph.class, reactionGraph.getId());
        assertNotNull(found);
        found.updateLastModifiedAt(LAST_MODIFIED_AT_NEWER);

        repository.update(found);
        flushAndClear();
        ReactionGraph updated = entityManager.find(ReactionGraph.class, reactionGraph.getId());
        assertNotNull(updated);
        assertThat(found.getLastModifiedAt())
                .isCloseTo(LAST_MODIFIED_AT_NEWER, within(1, ChronoUnit.MILLIS));
    }

    @Test
    void findByComponentId() {
        SystemComponent systemComponent = PersistenceTestUtility.createAndPersistSystemWithOneSystemComponent(entityManager);
        ReactionGraph reactionGraph = ReactionGraph.builder()
                .id(UUID.randomUUID())
                .systemComponent(systemComponent)
                .lastModifiedAt(LAST_MODIFIED_AT)
                .build();
        repository.add(reactionGraph);
        flushAndClear();

        Optional<ReactionGraph> found = repository.findByComponentId(systemComponent.getId());
        assertNotNull(found);
        assertTrue(found.isPresent());
    }

    @Test
    void findByComponentId_NotExisting() {
        SystemComponent systemComponent = PersistenceTestUtility.createAndPersistSystemWithOneSystemComponent(entityManager);
        flushAndClear();

        Optional<ReactionGraph> found = repository.findByComponentId(systemComponent.getId());
        assertNotNull(found);
        assertTrue(found.isEmpty());
    }

    @Test
    void findByComponentName() {
        SystemComponent systemComponent = PersistenceTestUtility.createAndPersistSystemWithOneSystemComponent(entityManager);
        ReactionGraph reactionGraph = ReactionGraph.builder()
                .id(UUID.randomUUID())
                .systemComponent(systemComponent)
                .lastModifiedAt(LAST_MODIFIED_AT)
                .build();
        repository.add(reactionGraph);
        flushAndClear();

        Optional<ReactionGraph> found = repository.findByComponentName(systemComponent.getName());
        assertNotNull(found);
        assertTrue(found.isPresent());
    }

    @Test
    void findByComponentName_NotExisting() {
        SystemComponent systemComponent = PersistenceTestUtility.createAndPersistSystemWithOneSystemComponent(entityManager);
        flushAndClear();

        Optional<ReactionGraph> found = repository.findByComponentName(systemComponent.getName());
        assertNotNull(found);
        assertTrue(found.isEmpty());
    }

    @Test
    void delete() {
        SystemComponent systemComponent = PersistenceTestUtility.createAndPersistSystemWithOneSystemComponent(entityManager);
        ReactionGraph reactionGraph = ReactionGraph.builder()
                .id(UUID.randomUUID())
                .systemComponent(systemComponent)
                .lastModifiedAt(LAST_MODIFIED_AT)
                .build();
        repository.add(reactionGraph);
        flushAndClear();

        ReactionGraph found = entityManager.find(ReactionGraph.class, reactionGraph.getId());
        assertNotNull(found);

        repository.delete(found);
        flushAndClear();

        ReactionGraph deleted = entityManager.find(ReactionGraph.class, reactionGraph.getId());
        assertNull(deleted);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
