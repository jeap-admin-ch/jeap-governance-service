package ch.admin.bit.jeap.governance.persistence;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.State;
import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


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

        createAndPersistSystemWithSystemComponent(systemComponent);

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

        createAndPersistSystemWithSystemComponent(systemComponent);

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

    private System createAndPersistSystemWithSystemComponent(SystemComponent systemComponent) {
        System system = System.builder()
                .name("Test System")
                .systemComponents(systemComponent == null ? List.of() : List.of(systemComponent))
                .state(State.OK)
                .aliases(Set.of("test-system"))
                .build();

        entityManager.persist(system);
        entityManager.flush();
        return system;
    }
}
