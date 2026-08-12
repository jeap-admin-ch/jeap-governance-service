package ch.admin.bit.jeap.governance.messagecontract.dataimport;

import ch.admin.bit.jeap.governance.domain.plugin.datasource.DataSourceImporter;
import ch.admin.bit.jeap.governance.messagecontract.connector.MessageContractConnector;
import ch.admin.bit.jeap.governance.messagecontract.synchronize.MessageContractVersionStatusSynchronizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(15)
@RequiredArgsConstructor
@Slf4j
public class MessageContractVersionImporter implements DataSourceImporter {
    private final MessageContractConnector connector;
    private final MessageContractVersionStatusSynchronizer synchronizer;

    @Override
    public void importData() {
        var statuses = connector.getVersionStatus();
        synchronizer.synchronize(statuses);
        log.info("Imported version status for {} deployed message contract(s)", statuses.size());
    }
}
