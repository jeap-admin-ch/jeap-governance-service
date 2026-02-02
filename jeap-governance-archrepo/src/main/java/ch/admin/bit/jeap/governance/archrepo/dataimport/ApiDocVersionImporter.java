package ch.admin.bit.jeap.governance.archrepo.dataimport;

import ch.admin.bit.jeap.governance.archrepo.connector.ArchRepoConnector;
import ch.admin.bit.jeap.governance.archrepo.connector.model.ApiDocVersionDto;
import ch.admin.bit.jeap.governance.archrepo.synchronize.ApiDocVersionSynchronizer;
import ch.admin.bit.jeap.governance.plugin.api.datasource.DataSourceImporter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

import static ch.admin.bit.jeap.governance.archrepo.dataimport.ImportOrder.API_DOC_VERSION_IMPORT_ORDER;

@Component
@Order(API_DOC_VERSION_IMPORT_ORDER)
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty("jeap.governance.archrepo.import.apidocversion.enabled")
public class ApiDocVersionImporter implements DataSourceImporter {

    private final ArchRepoConnector archRepoConnector;
    private final ApiDocVersionSynchronizer apiDocVersionSynchronizer;

    @Override
    public void importData() {
        log.info("Start synchronization with arch repo");
        List<ApiDocVersionDto> apiDocVersions = archRepoConnector.getApiDocVersions();
        log.debug("Got model from arch repo: {}", apiDocVersions);
        apiDocVersionSynchronizer.synchronizeModelWithArchRepo(apiDocVersions);
        log.info("Finished synchronization with arch repo");
    }
}
