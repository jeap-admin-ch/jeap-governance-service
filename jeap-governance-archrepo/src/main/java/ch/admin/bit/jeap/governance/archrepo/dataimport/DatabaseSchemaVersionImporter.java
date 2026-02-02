package ch.admin.bit.jeap.governance.archrepo.dataimport;

import ch.admin.bit.jeap.governance.archrepo.connector.ArchRepoConnector;
import ch.admin.bit.jeap.governance.archrepo.connector.model.DatabaseSchemaVersionDto;
import ch.admin.bit.jeap.governance.archrepo.synchronize.DatabaseSchemaVersionSynchronizer;
import ch.admin.bit.jeap.governance.plugin.api.datasource.DataSourceImporter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

import static ch.admin.bit.jeap.governance.archrepo.dataimport.ImportOrder.DATA_BASE_SCHEMA_VERSION_ORDER;

@Component
@Order(DATA_BASE_SCHEMA_VERSION_ORDER)
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty("jeap.governance.archrepo.import.databaseschemaversion.enabled")
public class DatabaseSchemaVersionImporter implements DataSourceImporter {

    private final ArchRepoConnector archRepoConnector;
    private final DatabaseSchemaVersionSynchronizer databaseSchemaVersionSynchronizer;

    @Override
    public void importData() {
        log.info("Start synchronization with arch repo");
        List<DatabaseSchemaVersionDto> databaseSchemaVersionDtos = archRepoConnector.getDatabaseSchemaVersions();
        log.debug("Got model from arch repo: {}", databaseSchemaVersionDtos);
        databaseSchemaVersionSynchronizer.synchronizeModelWithArchRepo(databaseSchemaVersionDtos);
        log.info("Finished synchronization with arch repo");
    }
}
