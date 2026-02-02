package ch.admin.bit.jeap.governance.archrepo.dataimport;

import ch.admin.bit.jeap.governance.archrepo.connector.ArchRepoConnector;
import ch.admin.bit.jeap.governance.archrepo.connector.model.RestApiRelationWithoutPactDto;
import ch.admin.bit.jeap.governance.archrepo.synchronize.RestApiRelationWithoutPactSynchronizer;
import ch.admin.bit.jeap.governance.plugin.api.datasource.DataSourceImporter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

import static ch.admin.bit.jeap.governance.archrepo.dataimport.ImportOrder.REST_API_RELATION_IMPORT_ORDER;

@Component
@Order(REST_API_RELATION_IMPORT_ORDER)
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty("jeap.governance.archrepo.import.restapirelationwithoutpact.enabled")
public class RestApiRelationWithoutPactImporter implements DataSourceImporter {

    private final ArchRepoConnector archRepoConnector;
    private final RestApiRelationWithoutPactSynchronizer restApiRelationWithoutPactSynchronizer;

    @Override
    public void importData() {
        log.info("Start synchronization with arch repo");
        List<RestApiRelationWithoutPactDto> restApiRelationDtos = archRepoConnector.getRestRelationWithoutPact();
        log.debug("Got model from arch repo: {}", restApiRelationDtos);
        restApiRelationWithoutPactSynchronizer.synchronizeModelWithArchRepo(restApiRelationDtos);
        log.info("Finished synchronization with arch repo");
    }
}
