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
import java.util.UUID;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(SystemRepositoryImpl.class)
class SystemRepositoryImplTest extends PostgresTestContainerBase {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SystemRepositoryImpl repository;


    @Test
    void findByName_shouldReturnSystem_whenExists() {
        System system = System.builder()
                .id(UUID.randomUUID())
                .name("My system")
                .systemComponents(List.of())
                .state(State.OK)
                .aliases(Set.of("my-system"))
                .build();

        repository.add(system);

        entityManager.flush();

        Optional<System> result = repository.findByName("My system");

        assertNotNull(result);
        assertTrue(result.isPresent());
        System systemResult = result.get();
        assertEquals("My system", systemResult.getName());
        assertEquals(State.OK, systemResult.getState());
        assertEquals(Set.of("my-system"), systemResult.getAliases());
        assertEquals(List.of(), systemResult.getSystemComponents());
        assertNotNull(systemResult.getCreatedAt());
    }

    @Test
    void findByName_shouldReturnSystem_whenExists_withSystemComponents() {
        System system = System.builder()
                .id(UUID.randomUUID())
                .name("My system")
                .systemComponents(List.of())
                .state(State.OK)
                .build();
        SystemComponent component1 = SystemComponent.builder()
                .id(UUID.randomUUID())
                .name("Component 1")
                .type(ComponentType.SELF_CONTAINED_SYSTEM)
                .state(State.OK)
                .build();
        SystemComponent component2 = SystemComponent.builder()
                .id(UUID.randomUUID())
                .name("Component 2")
                .type(ComponentType.BACKEND_SERVICE)
                .state(State.OK)
                .build();
        system.addSystemComponent(component1);
        system.addSystemComponent(component2);

        repository.add(system);

        entityManager.flush();

        Optional<System> result = repository.findByName("My system");

        assertNotNull(result);
        assertTrue(result.isPresent());
        System systemResult = result.get();
        assertEquals("My system", systemResult.getName());
        assertEquals(State.OK, systemResult.getState());
        assertEquals(Set.of(), systemResult.getAliases());
        assertEquals(2, systemResult.getSystemComponents().size());
        assertNotNull(systemResult.getCreatedAt());
    }

    @Test
    void update() {
        System system = System.builder()
                .id(UUID.randomUUID())
                .name("My system")
                .systemComponents(List.of())
                .state(State.OK)
                .aliases(Set.of("my-system"))
                .build();

        repository.add(system);

        entityManager.flush();

        Optional<System> result = repository.findByName("My system");

        assertNotNull(result);
        assertTrue(result.isPresent());
        System systemResult = result.get();
        systemResult.addSystemComponent(SystemComponent.builder()
                .id(UUID.randomUUID())
                .name("Component 1")
                .type(ComponentType.SELF_CONTAINED_SYSTEM)
                .state(State.OK)
                .build());

        repository.update(systemResult);
        entityManager.flush();

        Optional<System> updateResult = repository.findByName("My system");

        assertNotNull(updateResult);
        assertTrue(updateResult.isPresent());
        System updatedSystemResult = result.get();
        assertEquals(1, updatedSystemResult.getSystemComponents().size());
    }

    @Test
    void delete() {
        System system = System.builder()
                .id(UUID.randomUUID())
                .name("My system")
                .systemComponents(List.of())
                .state(State.OK)
                .aliases(Set.of("my-system"))
                .build();

        repository.add(system);

        entityManager.flush();

        Optional<System> result = repository.findByName("My system");

        assertNotNull(result);
        assertTrue(result.isPresent());
        System systemResult = result.get();

        repository.delete(systemResult);

        entityManager.flush();

        Optional<System> resultAfterDelete = repository.findByName("My system");

        assertNotNull(resultAfterDelete);
        assertFalse(resultAfterDelete.isPresent());
    }

    @Test
    void delete_withSystemComponents() {
        System system = System.builder()
                .id(UUID.randomUUID())
                .name("My system")
                .systemComponents(List.of())
                .state(State.OK)
                .build();
        SystemComponent component1 = SystemComponent.builder()
                .id(UUID.randomUUID())
                .name("Component 1")
                .type(ComponentType.SELF_CONTAINED_SYSTEM)
                .state(State.OK)
                .build();
        SystemComponent component2 = SystemComponent.builder()
                .id(UUID.randomUUID())
                .name("Component 2")
                .type(ComponentType.BACKEND_SERVICE)
                .state(State.OK)
                .build();
        system.addSystemComponent(component1);
        system.addSystemComponent(component2);

        repository.add(system);

        entityManager.flush();

        Optional<System> result = repository.findByName("My system");

        // Then
        assertNotNull(result);
        assertTrue(result.isPresent());
        System systemResult = result.get();

        repository.delete(systemResult);

        entityManager.flush();

        Optional<System> resultAfterDelete = repository.findByName("My system");

        assertNotNull(resultAfterDelete);
        assertFalse(resultAfterDelete.isPresent());
    }
}
