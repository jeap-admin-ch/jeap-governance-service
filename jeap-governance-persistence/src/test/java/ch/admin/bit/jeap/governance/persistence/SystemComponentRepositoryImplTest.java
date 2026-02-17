package ch.admin.bit.jeap.governance.persistence;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.rule.State;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;


@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(SystemComponentRepositoryImpl.class)
class SystemComponentRepositoryImplTest extends PostgresTestContainerBase {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SystemComponentRepositoryImpl repository;

    @Test
    void findByName() {
        SystemComponent systemComponent = SystemComponent.builder()
                .name("Test Component")
                .state(State.OK)
                .type(ComponentType.BACKEND_SERVICE)
                .build();

        createAndPersistSystemWithSystemComponents(systemComponent);

        Optional<SystemComponent> result = repository.findByName(systemComponent.getName());

        assertNotNull(result);
        assertTrue(result.isPresent());
        SystemComponent componentResult = result.get();
        assertEquals("Test Component", componentResult.getName());
        assertEquals(ComponentType.BACKEND_SERVICE, componentResult.getType());
        assertEquals(State.OK, componentResult.getState());
        assertNotNull(componentResult.getId());
        assertNotNull(componentResult.getCreatedAt());
    }

    @Test
    void findByName_emptyResult() {
        Optional<SystemComponent> result = repository.findByName("missing component");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void deleteById() {
        SystemComponent systemComponent = SystemComponent.builder()
                .name("Test Component")
                .state(State.OK)
                .type(ComponentType.BACKEND_SERVICE)
                .build();

        createAndPersistSystemWithSystemComponents(systemComponent);

        Optional<SystemComponent> result = repository.findByName(systemComponent.getName());
        assertNotNull(result);
        assertTrue(result.isPresent());

        repository.deleteById(result.get().getId());
        entityManager.flush();
        entityManager.clear();

        Optional<SystemComponent> afterDeletionResult = repository.findByName(systemComponent.getName());

        assertNotNull(afterDeletionResult);
        assertTrue(afterDeletionResult.isEmpty());
    }

    @Test
    void deleteById_NotExistingComponent() {
        assertDoesNotThrow(() -> repository.deleteById(42L));
    }

    @Test
    void findAllSystemComponentNames_returnsAllNames() {
        SystemComponent componentA = SystemComponent.builder()
                .name("Component A")
                .state(State.OK)
                .type(ComponentType.BACKEND_SERVICE)
                .build();
        SystemComponent componentB = SystemComponent.builder()
                .name("Component B")
                .state(State.OK)
                .type(ComponentType.BACKEND_SERVICE)
                .build();

        createAndPersistSystemWithSystemComponents(componentA, componentB);

        Set<String> result = repository.findAllSystemComponentNames();

        assertNotNull(result);
        assertEquals(Set.of("Component A", "Component B"), result);
    }

    @Test
    void findAllSystemComponentNames_emptyResult() {
        createAndPersistSystemWithSystemComponents();

        Set<String> result = repository.findAllSystemComponentNames();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void findSystemComponentNameById_returnsName() {
        SystemComponent componentA = SystemComponent.builder()
                .name("Component A")
                .state(State.OK)
                .type(ComponentType.BACKEND_SERVICE)
                .build();
        SystemComponent componentB = SystemComponent.builder()
                .name("Component B")
                .state(State.OK)
                .type(ComponentType.BACKEND_SERVICE)
                .build();
        System system = createAndPersistSystemWithSystemComponents(componentA, componentB);
        SystemComponent systemComponent = system.getSystemComponents().getFirst();

        Optional<String> result = repository.findSystemComponentNameById(systemComponent.getId());

        assertTrue(result.isPresent());
        assertEquals(systemComponent.getName(), result.get());
    }

    @Test
    void findSystemComponentNameById_emptyResult() {
        SystemComponent componentA = SystemComponent.builder()
                .name("Component A")
                .state(State.OK)
                .type(ComponentType.BACKEND_SERVICE)
                .build();
        System system = createAndPersistSystemWithSystemComponents(componentA);
        long existingId = system.getSystemComponents().getFirst().getId();
        long nonExistingId = existingId + 1; // there exists only one system component (existingId)

        Optional<String> result = repository.findSystemComponentNameById(nonExistingId);

        assertTrue(result.isEmpty());
    }

    @SuppressWarnings("UnusedReturnValue")
    private System createAndPersistSystemWithSystemComponents(SystemComponent... systemComponents) {
        List<SystemComponent> components = systemComponents == null ? List.of() : List.of(systemComponents);
        System system = System.builder()
                .name("Test System")
                .systemComponents(components)
                .state(State.OK)
                .aliases(Set.of("test-system"))
                .build();

        entityManager.persist(system);
        entityManager.flush();
        return system;
    }
}
