package ch.admin.bit.jeap.governance.archrepo.synchronize;

import ch.admin.bit.jeap.governance.archrepo.connector.model.ApiDocVersionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApiDocVersionSynchronizer {

    private final ApiDocVersionSystemSynchronizer apiDocVersionSystemSynchronizer;

    public void synchronizeModelWithArchRepo(List<ApiDocVersionDto> apiDocVersionDtos) {
        boolean hasException = false;
        // Group relations by provider system and component to minimize transaction size
        Map<String, List<ApiDocVersionDto>> apiDocVersionsBySystem = groupBySystem(apiDocVersionDtos);
        for (Map.Entry<String, List<ApiDocVersionDto>> entry : apiDocVersionsBySystem.entrySet()) {
            try {
                apiDocVersionSystemSynchronizer.synchronizeApiDocVersionWithArchRepo(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                // Log and continue with next system to avoid blocking the whole synchronization in case of errors
                log.error("Error synchronizing ApiDocVersions for system {}: {}. Proceeding import", entry.getKey(), e.getMessage(), e);
                hasException = true;
            }
        }
        if (hasException) {
            throw new ArchRepoSynchronizeException("Errors occurred during ApiDocVersion synchronization. Check logs for details.");
        }
    }

    private static Map<String, List<ApiDocVersionDto>> groupBySystem(List<ApiDocVersionDto> apiDocVersionDtos) {
        return apiDocVersionDtos.stream()
                .collect(Collectors.groupingBy(ApiDocVersionDto::getSystem));
    }
}
