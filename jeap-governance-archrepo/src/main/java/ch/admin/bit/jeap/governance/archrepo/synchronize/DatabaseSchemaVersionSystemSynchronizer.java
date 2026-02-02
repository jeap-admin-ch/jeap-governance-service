package ch.admin.bit.jeap.governance.archrepo.synchronize;

import ch.admin.bit.jeap.governance.archrepo.connector.model.DatabaseSchemaVersionDto;
import ch.admin.bit.jeap.governance.archrepo.domain.DatabaseSchemaVersion;
import ch.admin.bit.jeap.governance.archrepo.domain.DatabaseSchemaVersionRepository;
import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.SystemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSchemaVersionSystemSynchronizer {

    private final SystemRepository systemRepository;
    private final DatabaseSchemaVersionRepository databaseSchemaVersionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void synchronizeDatabaseSchemaVersionWithArchRepo(String systemName, List<DatabaseSchemaVersionDto> databaseSchemaVersionDtos) {
        Optional<System> systemOptional = systemRepository.findByName(systemName);
        if (systemOptional.isEmpty()) {
            // We do throw an exception here because the system must exist
            throw new ArchRepoSynchronizeException("System not found: " + systemName);
        }
        System system = systemOptional.get();
        databaseSchemaVersionRepository.deleteAllBySystemId(system.getId());
        addAllToSystem(system, databaseSchemaVersionDtos);
    }

    private void addAllToSystem(System system, List<DatabaseSchemaVersionDto> databaseSchemaVersionDtos) {
        for (DatabaseSchemaVersionDto databaseSchemaVersionDto : databaseSchemaVersionDtos) {
            Optional<SystemComponent> systemComponentByName = system.getSystemComponentByName(databaseSchemaVersionDto.getComponent());
            if (systemComponentByName.isEmpty()) {
                log.error("System component {} not found in system {}. Skipping API doc version synchronization for this component.", databaseSchemaVersionDto.getComponent(), system.getName());
                continue;
            }
            SystemComponent systemComponent = systemComponentByName.get();
            addToSystemComponent(databaseSchemaVersionDto, systemComponent);
        }
    }

    private void addToSystemComponent(DatabaseSchemaVersionDto databaseSchemaVersionDto, SystemComponent systemComponent) {
        log.info("Creating new DatabaseSchema version {} for system component {} with version: {}", databaseSchemaVersionDto.getVersion(), systemComponent.getName(), databaseSchemaVersionDto.getVersion());
        DatabaseSchemaVersion newVersion = DatabaseSchemaVersion.builder()
                .id(UUID.randomUUID())
                .systemComponent(systemComponent)
                .version(databaseSchemaVersionDto.getVersion())
                .build();
        databaseSchemaVersionRepository.add(newVersion);
    }
}
