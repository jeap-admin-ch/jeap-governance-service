package ch.admin.bit.jeap.governance.persistence;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.SystemReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
                .name("My system")
                .systemComponents(List.of())
                .aliases(Set.of("my-system"))
                .build();

        repository.add(system);

        entityManager.flush();

        Optional<System> result = repository.findByName("My system");

        assertNotNull(result);
        assertTrue(result.isPresent());
        System systemResult = result.get();
        assertEquals("My system", systemResult.getName());
        assertEquals(Set.of("my-system"), systemResult.getAliases());
        assertEquals(List.of(), systemResult.getSystemComponents());
        assertNotNull(systemResult.getCreatedAt());
    }

    @Test
    void findByName_shouldReturnSystem_whenExists_withSystemComponents() {
        System system = System.builder()
                .name("My system")
                .systemComponents(List.of())
                .build();
        SystemComponent component1 = SystemComponent.builder()
                .name("Component 1")
                .type(ComponentType.SELF_CONTAINED_SYSTEM)
                .build();
        SystemComponent component2 = SystemComponent.builder()
                .name("Component 2")
                .type(ComponentType.BACKEND_SERVICE)
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
        assertEquals(Set.of(), systemResult.getAliases());
        assertEquals(2, systemResult.getSystemComponents().size());
        assertNotNull(systemResult.getCreatedAt());
    }

    @Test
    void update() {
        System system = System.builder()
                .name("My system")
                .systemComponents(List.of())
                .aliases(Set.of("my-system"))
                .build();

        repository.add(system);

        entityManager.flush();

        Optional<System> result = repository.findByName("My system");

        assertNotNull(result);
        assertTrue(result.isPresent());
        System systemResult = result.get();
        systemResult.addSystemComponent(SystemComponent.builder()
                .name("Component 1")
                .type(ComponentType.SELF_CONTAINED_SYSTEM)
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
                .name("My system")
                .systemComponents(List.of())
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
                .name("My system")
                .systemComponents(List.of())
                .build();
        SystemComponent component1 = SystemComponent.builder()
                .name("Component 1")
                .type(ComponentType.SELF_CONTAINED_SYSTEM)
                .build();
        SystemComponent component2 = SystemComponent.builder()
                .name("Component 2")
                .type(ComponentType.BACKEND_SERVICE)
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

    @Test
    void findAllSystemReferences() {
        System system = System.builder()
                .name("My system")
                .systemComponents(List.of())
                .aliases(Set.of("my-system"))
                .build();

        entityManager.persist(system);
        entityManager.flush();
        entityManager.clear();

        List<SystemReference> allSystemReferences = repository.findAllSystemReferences();
        assertEquals(1, allSystemReferences.size());
        SystemReference systemReference = allSystemReferences.getFirst();
        assertEquals(system.getId(), systemReference.getId());
        assertEquals(system.getName(), systemReference.getName());
    }

    @Test
    void findAllSystemReferences_severalEntries() {
        System system1 = System.builder()
                .name("My system")
                .systemComponents(List.of())
                .aliases(Set.of("my-system"))
                .build();
        System system2 = System.builder()
                .name("My system 2")
                .systemComponents(List.of())
                .aliases(Set.of("my-system2"))
                .build();
        System system3 = System.builder()
                .name("My system 3")
                .systemComponents(List.of())
                .aliases(Set.of("my-system3"))
                .build();

        entityManager.persist(system1);
        entityManager.persist(system2);
        entityManager.persist(system3);
        entityManager.flush();
        entityManager.clear();

        List<SystemReference> allSystemReferences = repository.findAllSystemReferences();
        List<SystemReference> sorted = new ArrayList<>(allSystemReferences);
        sorted.sort(Comparator.comparing(SystemReference::getId));
        assertEquals(3, allSystemReferences.size());
        SystemReference systemReference1 = allSystemReferences.getFirst();
        assertEquals(system1.getId(), systemReference1.getId());
        assertEquals(system1.getName(), systemReference1.getName());
        SystemReference systemReference2 = allSystemReferences.get(1);
        assertEquals(system2.getId(), systemReference2.getId());
        assertEquals(system2.getName(), systemReference2.getName());
        SystemReference systemReference3 = allSystemReferences.get(2);
        assertEquals(system3.getId(), systemReference3.getId());
        assertEquals(system3.getName(), systemReference3.getName());
    }

}
