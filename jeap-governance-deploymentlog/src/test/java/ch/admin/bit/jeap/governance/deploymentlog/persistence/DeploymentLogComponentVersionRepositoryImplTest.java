package ch.admin.bit.jeap.governance.deploymentlog.persistence;

import ch.admin.bit.jeap.governance.deploymentlog.domain.DeploymentLogComponentVersion;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(DeploymentLogComponentVersionRepositoryImpl.class)
class DeploymentLogComponentVersionRepositoryImplTest extends PostgresTestContainerBase {

    private static final String VERSION_1_0_0 = "1.0.0";

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DeploymentLogComponentVersionRepositoryImpl repository;

    @Test
    void add() {
        SystemComponent systemComponent = PersistenceTestUtility.createAndPersistSystemWithOneSystemComponent(entityManager);

        DeploymentLogComponentVersion componentVersion = DeploymentLogComponentVersion.builder()
                .systemComponent(systemComponent)
                .version(VERSION_1_0_0)
                .createdAt(ZonedDateTime.now())
                .build();

        repository.add(componentVersion);
        flushAndClear();

        DeploymentLogComponentVersion found = entityManager.find(DeploymentLogComponentVersion.class, componentVersion.getId());
        assertNotNull(found);
        assertNotNull(found.getId());
        assertNotNull(found.getCreatedAt());
        assertEquals(VERSION_1_0_0, found.getVersion());
        assertEquals(systemComponent.getId(), found.getSystemComponent().getId());
    }

    @Test
    void findByComponentId() {
        SystemComponent systemComponent = PersistenceTestUtility.createAndPersistSystemWithOneSystemComponent(entityManager);
        DeploymentLogComponentVersion componentVersion = DeploymentLogComponentVersion.builder()
                .systemComponent(systemComponent)
                .version(VERSION_1_0_0)
                .build();

        repository.add(componentVersion);
        flushAndClear();

        Optional<DeploymentLogComponentVersion> found = repository.findByComponentId(systemComponent.getId());
        assertNotNull(found);
        assertTrue(found.isPresent());
    }

    @Test
    void findByComponentId_NotExisting() {
        SystemComponent systemComponent = PersistenceTestUtility.createAndPersistSystemWithOneSystemComponent(entityManager);
        flushAndClear();

        Optional<DeploymentLogComponentVersion> found = repository.findByComponentId(systemComponent.getId());
        assertNotNull(found);
        assertTrue(found.isEmpty());
    }

    @Test
    void findByComponentName() {
        SystemComponent systemComponent = PersistenceTestUtility.createAndPersistSystemWithOneSystemComponent(entityManager);
        DeploymentLogComponentVersion componentVersion = DeploymentLogComponentVersion.builder()
                .systemComponent(systemComponent)
                .version(VERSION_1_0_0)
                .build();
        repository.add(componentVersion);
        flushAndClear();

        Optional<DeploymentLogComponentVersion> found = repository.findByComponentName(systemComponent.getName());
        assertNotNull(found);
        assertTrue(found.isPresent());
    }

    @Test
    void findByComponentName_NotExisting() {
        SystemComponent systemComponent = PersistenceTestUtility.createAndPersistSystemWithOneSystemComponent(entityManager);
        flushAndClear();

        Optional<DeploymentLogComponentVersion> found = repository.findByComponentName(systemComponent.getName());
        assertNotNull(found);
        assertTrue(found.isEmpty());
    }

    @Test
    void delete() {
        SystemComponent systemComponent = PersistenceTestUtility.createAndPersistSystemWithOneSystemComponent(entityManager);
        DeploymentLogComponentVersion componentVersion = DeploymentLogComponentVersion.builder()
                .systemComponent(systemComponent)
                .version(VERSION_1_0_0)
                .build();
        repository.add(componentVersion);
        flushAndClear();

        DeploymentLogComponentVersion found = entityManager.find(DeploymentLogComponentVersion.class, componentVersion.getId());
        assertNotNull(found);

        repository.delete(found);
        flushAndClear();

        DeploymentLogComponentVersion deleted = entityManager.find(DeploymentLogComponentVersion.class, componentVersion.getId());
        assertNull(deleted);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }


}
