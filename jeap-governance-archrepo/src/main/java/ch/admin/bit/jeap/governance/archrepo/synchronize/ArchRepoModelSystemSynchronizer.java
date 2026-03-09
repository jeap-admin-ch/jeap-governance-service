package ch.admin.bit.jeap.governance.archrepo.synchronize;

import ch.admin.bit.jeap.governance.archrepo.connector.model.ArchRepoSystemDto;
import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.SystemComponentService;
import ch.admin.bit.jeap.governance.domain.SystemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class ArchRepoModelSystemSynchronizer {

    private final SystemService systemService;
    private final SystemComponentService systemComponentService;
    private final ArchRepoModelSystemUpdater archRepoModelSystemUpdater;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void synchronizeWithArchRepo(ArchRepoSystemDto archRepoSystem) {
        log.info("Synchronize system {}", archRepoSystem.getName());

        Optional<System> systemByName = systemService.findByName(archRepoSystem.getName());
        if (systemByName.isPresent()) {
            System existingSystem = systemByName.get();
            System updatedSystem = archRepoModelSystemUpdater.updateSystem(existingSystem, archRepoSystem, this::deleteSystemComponent);
            systemService.update(updatedSystem);
        } else {
            System newSystem = archRepoModelSystemUpdater.createNewSystem(archRepoSystem);
            systemService.add(newSystem);
        }
        log.info("Synchronize system done {}", archRepoSystem.getName());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteNoMoreExistingSystems(Set<String> allArchRepoSystemNames) {
        List<System> allSystems = systemService.findAll();
        allSystems.stream()
                .filter(system -> !allArchRepoSystemNames.contains(system.getName()))
                .forEach(system -> {
                    log.info("Delete system {} as its not in the arch repo", system.getName());
                    ArrayList<SystemComponent> systemComponentsCopy = new ArrayList<>(system.getSystemComponents());
                    // We have to inform listeners about the deletion of system components
                    for (SystemComponent systemComponent : systemComponentsCopy) {
                        log.info("Delete system component with name: {} because system {} will be deleted", systemComponent.getName(), system.getName());
                        deleteSystemComponent(systemComponent.getId());
                    }

                    systemService.deleteSystem(system);
                });
    }

    void deleteSystemComponent(long systemComponentId) {
        systemComponentService.deleteById(systemComponentId);
    }
}
