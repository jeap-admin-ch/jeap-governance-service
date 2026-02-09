package ch.admin.bit.jeap.governance.archrepo.synchronize;

import ch.admin.bit.jeap.governance.archrepo.connector.model.ApiDocVersionDto;
import ch.admin.bit.jeap.governance.archrepo.domain.ApiDocVersion;
import ch.admin.bit.jeap.governance.archrepo.domain.ApiDocVersionRepository;
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
public class ApiDocVersionSystemSynchronizer {

    private final SystemRepository systemRepository;
    private final ApiDocVersionRepository apiDocVersionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void synchronizeWithArchRepo(String systemName, List<ApiDocVersionDto> apiDocVersionDtos) {
        Optional<System> systemOptional = systemRepository.findByName(systemName);
        if (systemOptional.isEmpty()) {
            // We do throw an exception here because the system must exist
            throw new ArchRepoSynchronizeException("System not found: " + systemName);
        }
        System system = systemOptional.get();
        apiDocVersionRepository.deleteAllBySystemId(system.getId());
        addAllToSystem(system, apiDocVersionDtos);
    }

    private void addAllToSystem(System system, List<ApiDocVersionDto> apiDocVersionDtos) {
        for (ApiDocVersionDto apiDocVersionDto : apiDocVersionDtos) {
            Optional<SystemComponent> systemComponentByName = system.getSystemComponentByName(apiDocVersionDto.getComponent());
            if (systemComponentByName.isEmpty()) {
                log.error("System component {} not found in system {}. Skipping API doc version synchronization for this component.", apiDocVersionDto.getComponent(), system.getName());
                continue;
            }
            SystemComponent systemComponent = systemComponentByName.get();
            addToSystemComponent(apiDocVersionDto, systemComponent);
        }
    }

    private void addToSystemComponent(ApiDocVersionDto apiDocVersionDto, SystemComponent systemComponent) {
        log.info("Creating new API doc version {} for system component {} with version: {}", apiDocVersionDto.getVersion(), systemComponent.getName(), apiDocVersionDto.getVersion());
        ApiDocVersion newVersion = ApiDocVersion.builder()
                .id(UUID.randomUUID())
                .systemComponent(systemComponent)
                .version(apiDocVersionDto.getVersion())
                .build();
        apiDocVersionRepository.add(newVersion);
    }
}
