package ch.admin.bit.jeap.governance.archrepo.synchronize;

import ch.admin.bit.jeap.governance.archrepo.connector.model.ArchRepoSystemComponentDto;
import ch.admin.bit.jeap.governance.archrepo.connector.model.ArchRepoSystemComponentType;
import ch.admin.bit.jeap.governance.archrepo.connector.model.ArchRepoSystemDto;
import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.EntityFactory;
import ch.admin.bit.jeap.governance.domain.State;
import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class ArchRepoModelSystemUpdaterTest {

    private static final String OLD_COMPONENT = "Old Component";
    private static final String NEW_COMPONENT = "New Component";
    private static final String EXISTING_COMPONENT = "Existing Component";
    private static final AtomicLong SYSTEM_COMPONENT_ID_GENERATOR = new AtomicLong();

    private final EntityFactory entityFactory = new EntityFactory();

    private ArchRepoModelSystemUpdater archRepoModelSystemUpdater;

    @Test
    void createNewSystem() {
        String systemComponentName1 = "Component 1";
        String systemComponentName2 = "Component 2";
        Set<String> aliases = Set.of("SysA", "System Alpha");
        var archRepoSystem = ArchRepoSystemDto.builder().name("System A").aliases(aliases).systemComponents(Arrays.asList(ArchRepoSystemComponentDto.builder().name(systemComponentName1).type(ArchRepoSystemComponentType.BACKEND_SERVICE).build(), ArchRepoSystemComponentDto.builder().name(systemComponentName2).type(ArchRepoSystemComponentType.SELF_CONTAINED_SYSTEM).build())).build();

        System newSystem = archRepoModelSystemUpdater.createNewSystem(archRepoSystem);

        assertEquals(aliases, newSystem.getAliases());
        assertEquals(2, newSystem.getSystemComponents().size());

        Optional<SystemComponent> systemComponentName1ByNameOptional = newSystem.getSystemComponentByName(systemComponentName1);
        assertTrue(systemComponentName1ByNameOptional.isPresent());
        SystemComponent systemComponentName1ByName = systemComponentName1ByNameOptional.get();
        assertEquals(systemComponentName1, systemComponentName1ByName.getName());
        assertEquals(ComponentType.BACKEND_SERVICE, systemComponentName1ByName.getType()); // updated value
        assertEquals(State.OK, systemComponentName1ByName.getState());
        assertEquals(newSystem, systemComponentName1ByName.getSystem());
        assertNotNull(systemComponentName1ByName.getCreatedAt());

        Optional<SystemComponent> systemComponentName2ByNameOptional = newSystem.getSystemComponentByName(systemComponentName2);
        assertTrue(systemComponentName2ByNameOptional.isPresent());
        SystemComponent systemComponentName2ByName = systemComponentName2ByNameOptional.get();
        assertEquals(systemComponentName2, systemComponentName2ByName.getName());
        assertEquals(ComponentType.SELF_CONTAINED_SYSTEM, systemComponentName2ByName.getType()); // updated value
        assertEquals(State.OK, systemComponentName2ByName.getState());
        assertEquals(newSystem, systemComponentName2ByName.getSystem());
        assertNotNull(systemComponentName2ByName.getCreatedAt());
    }

    @Test
    void updateSystem_OneExistingSystemComponent() {
        System existingSystem = entityFactory.createNewSystem("System A", null);
        existingSystem.addSystemComponent(createNewSystemComponent(EXISTING_COMPONENT, ComponentType.SELF_CONTAINED_SYSTEM));
        LongConsumer systemComponentDeletionCallback = mock(LongConsumer.class);

        Set<String> aliases = Set.of("SysA", "System Alpha");
        var archRepoSystem = ArchRepoSystemDto.builder().name("System A").aliases(aliases).systemComponents(Arrays.asList(ArchRepoSystemComponentDto.builder().name(EXISTING_COMPONENT).type(ArchRepoSystemComponentType.BACKEND_SERVICE) // We change the type on purpose
                .build())).build();

        System updatedSystem = archRepoModelSystemUpdater.updateSystem(existingSystem, archRepoSystem, systemComponentDeletionCallback);
        assertEquals("System A", updatedSystem.getName());
        assertEquals(aliases, updatedSystem.getAliases());
        assertEquals(1, updatedSystem.getSystemComponents().size());

        Optional<SystemComponent> existingSystemComponentByNameOptional = updatedSystem.getSystemComponentByName(EXISTING_COMPONENT);
        assertTrue(existingSystemComponentByNameOptional.isPresent());
        SystemComponent existingSystemComponentByName = existingSystemComponentByNameOptional.get();
        assertEquals(EXISTING_COMPONENT, existingSystemComponentByName.getName());
        assertEquals(ComponentType.BACKEND_SERVICE, existingSystemComponentByName.getType()); // updated value
        assertEquals(State.OK, existingSystemComponentByName.getState());
        assertEquals(updatedSystem, existingSystemComponentByName.getSystem());
        assertNotNull(existingSystemComponentByName.getCreatedAt());
    }

    @Test
    void updateSystem_OneNewSystemComponent() {
        System existingSystem = entityFactory.createNewSystem("System A", null);
        LongConsumer systemComponentDeletionCallback = mock(LongConsumer.class);

        Set<String> aliases = Set.of("SysA", "System Alpha");
        var archRepoSystem = ArchRepoSystemDto.builder().name("System A").aliases(aliases).systemComponents(Arrays.asList(ArchRepoSystemComponentDto.builder().name(NEW_COMPONENT).type(ArchRepoSystemComponentType.SELF_CONTAINED_SYSTEM).build())).build();

        System updatedSystem = archRepoModelSystemUpdater.updateSystem(existingSystem, archRepoSystem, systemComponentDeletionCallback);
        assertEquals("System A", updatedSystem.getName());
        assertEquals(aliases, updatedSystem.getAliases());
        assertEquals(1, updatedSystem.getSystemComponents().size());

        Optional<SystemComponent> newSystemComponentByNameOptional = updatedSystem.getSystemComponentByName(NEW_COMPONENT);
        assertTrue(newSystemComponentByNameOptional.isPresent());
        SystemComponent newSystemComponentByName = newSystemComponentByNameOptional.get();
        assertEquals(NEW_COMPONENT, newSystemComponentByName.getName());
        assertEquals(ComponentType.SELF_CONTAINED_SYSTEM, newSystemComponentByName.getType());
        assertEquals(State.OK, newSystemComponentByName.getState());
        assertEquals(updatedSystem, newSystemComponentByName.getSystem());
        assertNotNull(newSystemComponentByName.getCreatedAt());
    }

    @Test
    void updateSystem_OneOldSystemComponent() {
        SystemComponent oldComponent = createNewSystemComponent(OLD_COMPONENT, ComponentType.BACKEND_SERVICE);
        System existingSystem = entityFactory.createNewSystem("System A", null);
        existingSystem.addSystemComponent(oldComponent);

        LongConsumer systemComponentDeletionCallback = mock(LongConsumer.class);

        Set<String> aliases = Set.of("SysA", "System Alpha");
        var archRepoSystem = ArchRepoSystemDto.builder().name("System A").aliases(aliases).systemComponents(List.of()).build();

        System updatedSystem = archRepoModelSystemUpdater.updateSystem(existingSystem, archRepoSystem, systemComponentDeletionCallback);
        assertEquals("System A", updatedSystem.getName());
        assertEquals(aliases, updatedSystem.getAliases());
        assertEquals(0, updatedSystem.getSystemComponents().size());

        Optional<SystemComponent> oldSystemComponentByNameOptional = updatedSystem.getSystemComponentByName(OLD_COMPONENT);
        assertFalse(oldSystemComponentByNameOptional.isPresent());

        verify(systemComponentDeletionCallback).accept(oldComponent.getId());
        verifyNoMoreInteractions(systemComponentDeletionCallback);
    }

    @Test
    void updateSystem_OneNewOneExistingAndOneOldSystemComponent() {
        SystemComponent oldComponent = createNewSystemComponent(OLD_COMPONENT, ComponentType.BACKEND_SERVICE);
        System existingSystem = entityFactory.createNewSystem("System A", null);
        existingSystem.addSystemComponent(createNewSystemComponent(EXISTING_COMPONENT, ComponentType.SELF_CONTAINED_SYSTEM));
        existingSystem.addSystemComponent(oldComponent);

        LongConsumer systemComponentDeletionCallback = mock(LongConsumer.class);

        Set<String> aliases = Set.of("SysA", "System Alpha");
        var archRepoSystem = ArchRepoSystemDto.builder().name("System A").aliases(aliases).systemComponents(Arrays.asList(ArchRepoSystemComponentDto.builder().name(EXISTING_COMPONENT).type(ArchRepoSystemComponentType.BACKEND_SERVICE) // We change the type on purpose
                .build(), ArchRepoSystemComponentDto.builder().name(NEW_COMPONENT).type(ArchRepoSystemComponentType.SELF_CONTAINED_SYSTEM).build())).build();

        System updatedSystem = archRepoModelSystemUpdater.updateSystem(existingSystem, archRepoSystem, systemComponentDeletionCallback);
        assertEquals("System A", updatedSystem.getName());
        assertEquals(aliases, updatedSystem.getAliases());
        assertEquals(2, updatedSystem.getSystemComponents().size());

        Optional<SystemComponent> existingSystemComponentByNameOptional = updatedSystem.getSystemComponentByName(EXISTING_COMPONENT);
        assertTrue(existingSystemComponentByNameOptional.isPresent());
        SystemComponent existingSystemComponentByName = existingSystemComponentByNameOptional.get();
        assertEquals(EXISTING_COMPONENT, existingSystemComponentByName.getName());
        assertEquals(ComponentType.BACKEND_SERVICE, existingSystemComponentByName.getType()); // updated value
        assertEquals(State.OK, existingSystemComponentByName.getState());
        assertEquals(updatedSystem, existingSystemComponentByName.getSystem());
        assertNotNull(existingSystemComponentByName.getCreatedAt());

        Optional<SystemComponent> newSystemComponentByNameOptional = updatedSystem.getSystemComponentByName(NEW_COMPONENT);
        assertTrue(newSystemComponentByNameOptional.isPresent());
        SystemComponent newSystemComponentByName = newSystemComponentByNameOptional.get();
        assertEquals(NEW_COMPONENT, newSystemComponentByName.getName());
        assertEquals(ComponentType.SELF_CONTAINED_SYSTEM, newSystemComponentByName.getType());
        assertEquals(State.OK, newSystemComponentByName.getState());
        assertEquals(updatedSystem, newSystemComponentByName.getSystem());
        assertNotNull(newSystemComponentByName.getCreatedAt());

        Optional<SystemComponent> oldSystemComponentByNameOptional = updatedSystem.getSystemComponentByName(OLD_COMPONENT);
        assertFalse(oldSystemComponentByNameOptional.isPresent());

        verify(systemComponentDeletionCallback).accept(oldComponent.getId());
        verifyNoMoreInteractions(systemComponentDeletionCallback);
    }

    private SystemComponent createNewSystemComponent(String existingComponent, ComponentType componentType) {
        SystemComponent systemComponent = entityFactory.createNewSystemComponent(existingComponent, componentType);
        ReflectionTestUtils.setField(systemComponent, "id", SYSTEM_COMPONENT_ID_GENERATOR.incrementAndGet());
        return systemComponent;
    }

    @BeforeEach
    void setUp() {
        archRepoModelSystemUpdater = new ArchRepoModelSystemUpdater(entityFactory);
    }
}
