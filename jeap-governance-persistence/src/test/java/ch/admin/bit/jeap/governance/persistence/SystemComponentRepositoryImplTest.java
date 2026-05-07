package ch.admin.bit.jeap.governance.persistence;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.SystemComponentReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
                .type(ComponentType.BACKEND_SERVICE)
                .build();

        createAndPersistSystemWithSystemComponents(systemComponent);

        Optional<SystemComponent> result = repository.findByName(systemComponent.getName());

        assertNotNull(result);
        assertTrue(result.isPresent());
        SystemComponent componentResult = result.get();
        assertEquals("Test Component", componentResult.getName());
        assertEquals(ComponentType.BACKEND_SERVICE, componentResult.getType());
        assertNotNull(componentResult.getId());
        assertNotNull(componentResult.getCreatedAt());
    }

    @Test
    void findByName_lowerCase() {
        SystemComponent systemComponent = SystemComponent.builder()
                .name("Test Component")
                .type(ComponentType.BACKEND_SERVICE)
                .build();

        createAndPersistSystemWithSystemComponents(systemComponent);

        Optional<SystemComponent> result = repository.findByName(systemComponent.getName().toLowerCase());

        assertNotNull(result);
        assertTrue(result.isPresent());
        SystemComponent componentResult = result.get();
        assertEquals("Test Component", componentResult.getName());
        assertEquals(ComponentType.BACKEND_SERVICE, componentResult.getType());
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
    void findAllSystemComponentReferences_returnsAllReferences() {
        SystemComponent componentA = SystemComponent.builder()
                .name("Component A")
                .type(ComponentType.BACKEND_SERVICE)
                .build();
        SystemComponent componentB = SystemComponent.builder()
                .name("Component B")
                .type(ComponentType.BACKEND_SERVICE)
                .build();

        createAndPersistSystemWithSystemComponents(componentA, componentB);

        List<SystemComponentReference> result = repository.findAllSystemComponentReferences();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(
                Set.of("Component A", "Component B"),
                result.stream().map(SystemComponentReference::getName).collect(Collectors.toSet()));
        result.forEach(ref -> assertTrue(ref.getId() > 0));
    }

    @Test
    void findAllSystemComponentReferences_emptyResult() {
        createAndPersistSystemWithSystemComponents();

        List<SystemComponentReference> result = repository.findAllSystemComponentReferences();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    private void createAndPersistSystemWithSystemComponents(SystemComponent... systemComponents) {
        List<SystemComponent> components = systemComponents == null ? List.of() : List.of(systemComponents);
        System system = System.builder()
                .name("Test System")
                .systemComponents(components)
                .aliases(Set.of("test-system"))
                .build();

        entityManager.persist(system);
        entityManager.flush();
    }
}
