package ch.admin.bit.jeap.governance.messagecontract.synchronize;

import ch.admin.bit.jeap.governance.messagecontract.connector.MessageContractVersionStatusDto;
import ch.admin.bit.jeap.governance.messagecontract.domain.MessageContractVersionState;
import ch.admin.bit.jeap.governance.messagecontract.domain.MessageContractVersionStatus;
import ch.admin.bit.jeap.governance.messagecontract.domain.MessageContractVersionStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MessageContractVersionStatusSynchronizer {
    private final MessageContractVersionStatusRepository repository;

    public void synchronize(List<MessageContractVersionStatusDto> statuses) {
        List<MessageContractVersionStatus> entities = statuses.stream()
                .map(status -> new MessageContractVersionStatus(status.appName(), status.appVersion(),
                        status.messageType(), status.usedVersion(), status.latestVersion(), status.topic(),
                        status.role(), stateOf(status)))
                .toList();
        repository.replaceSnapshot(entities);
    }

    /**
     * Older Message Contract Service versions do not report a status; it is then derived from {@code upToDate}. A
     * contract without latest version or with a status unknown to this version cannot be evaluated.
     */
    public static MessageContractVersionState stateOf(MessageContractVersionStatusDto status) {
        if (status.latestVersion() == null) {
            return MessageContractVersionState.UNKNOWN;
        }
        if (status.status() == null) {
            return status.upToDate() ? MessageContractVersionState.UP_TO_DATE : MessageContractVersionState.OUTDATED;
        }
        return Arrays.stream(MessageContractVersionState.values())
                .filter(state -> state.name().equals(status.status()))
                .findFirst()
                .orElse(MessageContractVersionState.UNKNOWN);
    }
}
