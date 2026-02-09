package ch.admin.bit.jeap.governance.archrepo.dataimport;

import ch.admin.bit.jeap.governance.archrepo.connector.ArchRepoConnector;
import ch.admin.bit.jeap.governance.archrepo.connector.model.ArchRepoModelDto;
import ch.admin.bit.jeap.governance.archrepo.synchronize.ArchRepoModelSynchronizer;
import ch.admin.bit.jeap.governance.plugin.api.datasource.DataSourceImporter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import static ch.admin.bit.jeap.governance.archrepo.dataimport.ImportOrder.SYSTEM_IMPORT_ORDER;

/**
 * Import systems and their system components from ArchRepo data source.
 * Must be the first in order to ensure proper data import sequence.
 */
@Component
@Order(SYSTEM_IMPORT_ORDER)
@RequiredArgsConstructor
@Slf4j
public class ArchRepoSystemImporter implements DataSourceImporter {

    private final ArchRepoConnector archRepoConnector;
    private final ArchRepoModelSynchronizer archRepoModelSynchronizer;

    @Override
    public void importData() {
        log.info("Start synchronization with arch repo");
        ArchRepoModelDto archRepoModel = archRepoConnector.getModelFromArchRepo();
        log.debug("Got model from arch repo: {}", archRepoModel);
        archRepoModelSynchronizer.synchronizeWithArchRepo(archRepoModel);
        log.info("Finished synchronization with arch repo");
    }
}
