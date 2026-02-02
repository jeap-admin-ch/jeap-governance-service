package ch.admin.bit.jeap.governance.archrepo.synchronize;

import ch.admin.bit.jeap.governance.archrepo.connector.model.DatabaseSchemaVersionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSchemaVersionSynchronizer {

    private final DatabaseSchemaVersionSystemSynchronizer databaseSchemaVersionSystemSynchronizer;

    public void synchronizeModelWithArchRepo(List<DatabaseSchemaVersionDto> databaseSchemaVersionDtos) {
        boolean hasException = false;
        // Group relations by provider system and component to minimize transaction size
        Map<String, List<DatabaseSchemaVersionDto>> databaseSchemaVersionsBySystem = groupBySystem(databaseSchemaVersionDtos);
        for (Map.Entry<String, List<DatabaseSchemaVersionDto>> entry : databaseSchemaVersionsBySystem.entrySet()) {
            try {
                databaseSchemaVersionSystemSynchronizer.synchronizeDatabaseSchemaVersionWithArchRepo(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                // Log and continue with next system to avoid blocking the whole synchronization in case of errors
                log.error("Error synchronizing DatabaseSchemaVersions for system {}: {}. Proceeding import", entry.getKey(), e.getMessage(), e);
                hasException = true;
            }
        }
        if (hasException) {
            throw new ArchRepoSynchronizeException("Errors occurred during DatabaseSchemaVersion synchronization. Check logs for details.");
        }
    }

    private static Map<String, List<DatabaseSchemaVersionDto>> groupBySystem(List<DatabaseSchemaVersionDto> databaseSchemaVersionDtos) {
        return databaseSchemaVersionDtos.stream()
                .collect(Collectors.groupingBy(DatabaseSchemaVersionDto::getSystem));
    }
}
