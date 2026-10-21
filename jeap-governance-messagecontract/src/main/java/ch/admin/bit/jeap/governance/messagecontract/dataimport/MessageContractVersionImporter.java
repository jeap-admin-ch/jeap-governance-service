package ch.admin.bit.jeap.governance.messagecontract.dataimport;

import ch.admin.bit.jeap.governance.domain.plugin.datasource.DataSourceImporter;
import ch.admin.bit.jeap.governance.messagecontract.connector.MessageContractConnector;
import ch.admin.bit.jeap.governance.messagecontract.connector.MessageContractVersionStatusDto;
import ch.admin.bit.jeap.governance.messagecontract.domain.MessageContractVersionState;
import ch.admin.bit.jeap.governance.messagecontract.synchronize.MessageContractVersionStatusSynchronizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

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
        List<MessageContractVersionStatusDto> unknown = statuses.stream()
                .filter(status -> MessageContractVersionStatusSynchronizer.stateOf(status) == MessageContractVersionState.UNKNOWN)
                .toList();
        // Contracts whose latest version could not be determined are imported anyway, so that a single broken
        // contract does not prevent the import of all others. The rule does not evaluate them.
        unknown.forEach(status -> log.warn(
                "Latest message version of {}:{} message type {} ({} on {}) is unknown; the contract is not evaluated",
                status.appName(), status.appVersion(), status.messageType(), status.role(), status.topic()));
        synchronizer.synchronize(statuses);
        log.info("Imported version status for {} deployed message contract(s), {} with unknown latest version",
                statuses.size(), unknown.size());
    }
}
