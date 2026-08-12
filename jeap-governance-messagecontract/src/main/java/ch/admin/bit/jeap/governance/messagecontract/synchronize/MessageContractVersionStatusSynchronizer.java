package ch.admin.bit.jeap.governance.messagecontract.synchronize;

import ch.admin.bit.jeap.governance.messagecontract.connector.MessageContractVersionStatusDto;
import ch.admin.bit.jeap.governance.messagecontract.domain.MessageContractVersionStatus;
import ch.admin.bit.jeap.governance.messagecontract.domain.MessageContractVersionStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MessageContractVersionStatusSynchronizer {
    private final MessageContractVersionStatusRepository repository;

    public void synchronize(List<MessageContractVersionStatusDto> statuses) {
        List<MessageContractVersionStatus> entities = statuses.stream()
                .map(status -> new MessageContractVersionStatus(status.appName(), status.appVersion(),
                        status.messageType(), status.usedVersion(), status.latestVersion(), status.topic(),
                        status.role(), status.upToDate()))
                .toList();
        repository.replaceSnapshot(entities);
    }
}
