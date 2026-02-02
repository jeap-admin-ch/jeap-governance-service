package ch.admin.bit.jeap.governance.archrepo.synchronize;

import ch.admin.bit.jeap.governance.archrepo.connector.model.ArchRepoSystemComponentDto;
import ch.admin.bit.jeap.governance.archrepo.connector.model.ArchRepoSystemDto;
import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.EntityFactory;
import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static java.util.stream.Collectors.toSet;

@Component
@RequiredArgsConstructor
@Slf4j
public class ArchRepoModelSystemUpdater {

    private final EntityFactory entityFactory;

    System updateSystem(System existingSystem, ArchRepoSystemDto archRepoSystem, Consumer<UUID> systemComponentDeletionCallback) {
        log.debug("Synchronize system {}", existingSystem.getName());
        existingSystem.setAliases(archRepoSystem.getAliases());
        List<ArchRepoSystemComponentDto> systemComponentDtos = archRepoSystem.getSystemComponents();

        // Update existing services
        systemComponentDtos.stream()
                .filter(systemComponent -> existingSystem.getSystemComponentByName(systemComponent.getName()).isPresent())
                .forEach(systemComponent -> update(existingSystem, systemComponent));

        // Delete removed services (copyOf to avoid concurrent mod. exceptions when removing services)
        List<SystemComponent> services = List.copyOf(existingSystem.getSystemComponents());
        Set<String> systemComponentNames = systemComponentDtos.stream()
                .map(ArchRepoSystemComponentDto::getName)
                .collect(toSet());
        services.stream()
                .filter(existingSystemComponent -> !systemComponentNames.contains(existingSystemComponent.getName()))
                .forEach(systemComponent -> deleteSystemComponent(existingSystem, systemComponent, systemComponentDeletionCallback));

        // Add new services
        systemComponentDtos.stream()
                .filter(systemComponent -> existingSystem.getSystemComponentByName(systemComponent.getName()).isEmpty())
                .map(this::createNewSystemComponent)
                .forEach(existingSystem::addSystemComponent);

        return existingSystem;
    }

    System createNewSystem(ArchRepoSystemDto archRepoSystem) {
        String name = archRepoSystem.getName();
        Set<String> aliases = archRepoSystem.getAliases();
        System system = entityFactory.createNewSystem(name, aliases);
        archRepoSystem.getSystemComponents().stream()
                .map(this::createNewSystemComponent)
                .forEach(system::addSystemComponent);
        return system;
    }

    private void deleteSystemComponent(System system, SystemComponent systemComponent, Consumer<UUID> systemComponentDeletionCallback) {
        log.info("Delete systemComponent with name: {} and id: {} as its not in the arch repo", systemComponent.getName(), systemComponent.getId());
        systemComponentDeletionCallback.accept(systemComponent.getId());
        // just to make sure system component is removed from system
        system.deleteSystemComponent(systemComponent);
    }

    private void update(System system, ArchRepoSystemComponentDto systemComponent) {
        log.info("Update existing service {}", systemComponent.getName());
        SystemComponent serviceByName = system.getSystemComponentByName(systemComponent.getName()).orElseThrow();
        serviceByName.update(ComponentType.valueOf(systemComponent.getType().name()));
    }


    private SystemComponent createNewSystemComponent(ArchRepoSystemComponentDto systemComponent) {
        log.info("Create service {} as it is in the arch repo", systemComponent.getName());
        return entityFactory.createNewSystemComponent(systemComponent.getName(), ComponentType.valueOf(systemComponent.getType().name()));
    }
}
