package ch.admin.bit.jeap.governance.archrepo.persistence;

import ch.admin.bit.jeap.governance.archrepo.domain.DatabaseSchemaVersion;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static ch.admin.bit.jeap.governance.archrepo.persistence.PersistenceTestUtility.createAndPersistSystemWithTwoSystemComponents;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(DatabaseSchemaVersionRepositoryImpl.class)
class DatabaseSchemaVersionRepositoryImplTest extends PostgresTestContainerBase {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JpaDatabaseSchemaVersionRepository jpaRepository;

    @Autowired
    private DatabaseSchemaVersionRepositoryImpl repository;

    @Test
    void add() {
        SystemComponent systemComponent = PersistenceTestUtility.createAndPersistSystemWithOneSystemComponent(entityManager);

        DatabaseSchemaVersion databaseSchemaVersion = DatabaseSchemaVersion.builder()
                .systemComponent(systemComponent)
                .version("1.0.0")
                .build();

        repository.add(databaseSchemaVersion);
        flushAndClear();

        DatabaseSchemaVersion found = entityManager.find(DatabaseSchemaVersion.class, databaseSchemaVersion.getId());
        assertNotNull(found);
        assertNotNull(found.getId());
        assertNotNull(found.getCreatedAt());
        assertEquals("1.0.0", found.getVersion());
        assertEquals(systemComponent.getId(), found.getSystemComponent().getId());
    }

    @Test
    void findByComponentId() {
        SystemComponent systemComponent = PersistenceTestUtility.createAndPersistSystemWithOneSystemComponent(entityManager);
        DatabaseSchemaVersion databaseSchemaVersion = DatabaseSchemaVersion.builder()
                .systemComponent(systemComponent)
                .version("1.0.0")
                .build();
        repository.add(databaseSchemaVersion);
        flushAndClear();

        Optional<DatabaseSchemaVersion> found = repository.findByComponentId(systemComponent.getId());
        assertNotNull(found);
        assertTrue(found.isPresent());
    }

    @Test
    void findByComponentId_NotExisting() {
        SystemComponent systemComponent = PersistenceTestUtility.createAndPersistSystemWithOneSystemComponent(entityManager);
        flushAndClear();

        Optional<DatabaseSchemaVersion> found = repository.findByComponentId(systemComponent.getId());
        assertNotNull(found);
        assertTrue(found.isEmpty());
    }

    @Test
    void delete() {
        SystemComponent systemComponent = PersistenceTestUtility.createAndPersistSystemWithOneSystemComponent(entityManager);
        DatabaseSchemaVersion databaseSchemaVersion = DatabaseSchemaVersion.builder()
                .systemComponent(systemComponent)
                .version("1.0.0")
                .build();
        repository.add(databaseSchemaVersion);
        flushAndClear();

        DatabaseSchemaVersion found = entityManager.find(DatabaseSchemaVersion.class, databaseSchemaVersion.getId());
        assertNotNull(found);

        repository.delete(found);
        flushAndClear();

        DatabaseSchemaVersion deleted = entityManager.find(DatabaseSchemaVersion.class, databaseSchemaVersion.getId());
        assertNull(deleted);
    }

    @Test
    void deleteAllBySystemId() {
        PersistenceTestUtility.TwoSystemComponents systemComponentsSystem1 = createAndPersistSystemWithTwoSystemComponents("TestSystem1", entityManager);
        PersistenceTestUtility.TwoSystemComponents systemComponentsSystem2 = createAndPersistSystemWithTwoSystemComponents("TestSystem2", entityManager);
        SystemComponent systemComponent11 = systemComponentsSystem1.first();
        SystemComponent systemComponent12 = systemComponentsSystem1.second();
        SystemComponent systemComponent21 = systemComponentsSystem2.first();

        DatabaseSchemaVersion databaseSchemaVersion11 = DatabaseSchemaVersion.builder()
                .systemComponent(systemComponent11)
                .version("1.0.0")
                .build();
        DatabaseSchemaVersion databaseSchemaVersion12 = DatabaseSchemaVersion.builder()
                .systemComponent(systemComponent12)
                .version("1.0.0")
                .build();
        DatabaseSchemaVersion databaseSchemaVersion21 = DatabaseSchemaVersion.builder()
                .systemComponent(systemComponent21)
                .version("1.0.0")
                .build();
        repository.add(databaseSchemaVersion11);
        repository.add(databaseSchemaVersion12);
        repository.add(databaseSchemaVersion21);
        flushAndClear();


        Iterable<DatabaseSchemaVersion> all = jpaRepository.findAll();
        assertThat(all).hasSize(3);

        repository.deleteAllBySystemId(systemComponent11.getSystem().getId());
        flushAndClear();

        Iterable<DatabaseSchemaVersion> allAfterDeletion = jpaRepository.findAll();
        assertThat(allAfterDeletion).hasSize(1);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
