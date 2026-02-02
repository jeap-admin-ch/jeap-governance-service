package ch.admin.bit.jeap.governance.archrepo.persistence;

import ch.admin.bit.jeap.governance.archrepo.domain.ApiDocVersion;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.Optional;
import java.util.UUID;

import static ch.admin.bit.jeap.governance.archrepo.persistence.PersistenceTestUtility.createAndPersistSystemWithOneSystemComponent;
import static ch.admin.bit.jeap.governance.archrepo.persistence.PersistenceTestUtility.createAndPersistSystemWithTwoSystemComponents;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ApiDocVersionRepositoryImpl.class)
class ApiDocVersionRepositoryImplTest extends PostgresTestContainerBase {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ApiDocVersionRepositoryImpl repository;

    @Autowired
    private JpaApiDocVersionRepository jpaRepository;

    @Test
    void add() {
        SystemComponent systemComponent = createAndPersistSystemWithOneSystemComponent(entityManager);

        ApiDocVersion apiDocVersion = ApiDocVersion.builder()
                .id(UUID.randomUUID())
                .systemComponent(systemComponent)
                .version("1.0.0")
                .build();

        repository.add(apiDocVersion);
        flushAndClear();

        ApiDocVersion found = entityManager.find(ApiDocVersion.class, apiDocVersion.getId());
        assertNotNull(found);
        assertNotNull(found.getId());
        assertNotNull(found.getCreatedAt());
        assertEquals("1.0.0", found.getVersion());
        assertEquals(systemComponent.getId(), found.getSystemComponent().getId());
    }

    @Test
    void findByComponentId() {
        SystemComponent systemComponent = createAndPersistSystemWithOneSystemComponent(entityManager);
        ApiDocVersion apiDocVersion = ApiDocVersion.builder()
                .id(UUID.randomUUID())
                .systemComponent(systemComponent)
                .version("1.0.0")
                .build();
        repository.add(apiDocVersion);
        flushAndClear();

        Optional<ApiDocVersion> found = repository.findByComponentId(systemComponent.getId());
        assertNotNull(found);
        assertTrue(found.isPresent());
    }

    @Test
    void findByComponentId_NotExisting() {
        SystemComponent systemComponent = createAndPersistSystemWithOneSystemComponent(entityManager);
        flushAndClear();

        Optional<ApiDocVersion> found = repository.findByComponentId(systemComponent.getId());
        assertNotNull(found);
        assertTrue(found.isEmpty());
    }

    @Test
    void delete() {
        SystemComponent systemComponent = createAndPersistSystemWithOneSystemComponent(entityManager);
        ApiDocVersion apiDocVersion = ApiDocVersion.builder()
                .id(UUID.randomUUID())
                .systemComponent(systemComponent)
                .version("1.0.0")
                .build();
        repository.add(apiDocVersion);
        flushAndClear();

        ApiDocVersion found = entityManager.find(ApiDocVersion.class, apiDocVersion.getId());
        assertNotNull(found);

        repository.delete(found);
        flushAndClear();

        ApiDocVersion deleted = entityManager.find(ApiDocVersion.class, apiDocVersion.getId());
        assertNull(deleted);
    }

    @Test
    void deleteAllBySystemId() {
        PersistenceTestUtility.TwoSystemComponents systemComponentsSystem1 = createAndPersistSystemWithTwoSystemComponents("TestSystem1", entityManager);
        PersistenceTestUtility.TwoSystemComponents systemComponentsSystem2 = createAndPersistSystemWithTwoSystemComponents("TestSystem2", entityManager);
        SystemComponent systemComponent11 = systemComponentsSystem1.first();
        SystemComponent systemComponent12 = systemComponentsSystem1.second();
        SystemComponent systemComponent21 = systemComponentsSystem2.first();

        ApiDocVersion apiDocVersion11 = ApiDocVersion.builder()
                .id(UUID.randomUUID())
                .systemComponent(systemComponent11)
                .version("1.0.0")
                .build();
        ApiDocVersion apiDocVersion12 = ApiDocVersion.builder()
                .id(UUID.randomUUID())
                .systemComponent(systemComponent12)
                .version("1.0.0")
                .build();
        ApiDocVersion apiDocVersion21 = ApiDocVersion.builder()
                .id(UUID.randomUUID())
                .systemComponent(systemComponent21)
                .version("1.0.0")
                .build();
        repository.add(apiDocVersion11);
        repository.add(apiDocVersion12);
        repository.add(apiDocVersion21);
        flushAndClear();

        Iterable<ApiDocVersion> all = jpaRepository.findAll();
        assertThat(all).hasSize(3);

        repository.deleteAllBySystemId(systemComponent11.getSystem().getId());
        flushAndClear();

        Iterable<ApiDocVersion> allAfterDeletion = jpaRepository.findAll();
        assertThat(allAfterDeletion).hasSize(1);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
