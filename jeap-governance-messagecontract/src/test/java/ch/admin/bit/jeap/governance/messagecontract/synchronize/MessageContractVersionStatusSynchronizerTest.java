package ch.admin.bit.jeap.governance.messagecontract.synchronize;

import ch.admin.bit.jeap.governance.messagecontract.connector.MessageContractVersionStatusDto;
import ch.admin.bit.jeap.governance.messagecontract.domain.MessageContractVersionState;
import ch.admin.bit.jeap.governance.messagecontract.domain.MessageContractVersionStatus;
import ch.admin.bit.jeap.governance.messagecontract.domain.MessageContractVersionStatusRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MessageContractVersionStatusSynchronizerTest {
    private final MessageContractVersionStatusRepository repository = mock(MessageContractVersionStatusRepository.class);
    private final MessageContractVersionStatusSynchronizer synchronizer = new MessageContractVersionStatusSynchronizer(repository);

    @Test
    void replacesSnapshotIncludingUnknownContracts() {
        synchronizer.synchronize(List.of(
                dto("1.0.0", "2.0.0", false, "OUTDATED"),
                dto("1.0.0", null, false, "UNKNOWN"),
                dto("2.0.0", "2.0.0", true, "UP_TO_DATE")));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MessageContractVersionStatus>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).replaceSnapshot(captor.capture());
        assertThat(captor.getValue())
                .extracting(MessageContractVersionStatus::getState)
                .containsExactly(MessageContractVersionState.OUTDATED, MessageContractVersionState.UNKNOWN,
                        MessageContractVersionState.UP_TO_DATE);
    }

    @Test
    void derivesStateFromUpToDateForResponsesWithoutStatus() {
        assertThat(MessageContractVersionStatusSynchronizer.stateOf(dto("1.0.0", "1.0.0", true, null)))
                .isEqualTo(MessageContractVersionState.UP_TO_DATE);
        assertThat(MessageContractVersionStatusSynchronizer.stateOf(dto("1.0.0", "2.0.0", false, null)))
                .isEqualTo(MessageContractVersionState.OUTDATED);
    }

    @Test
    void treatsMissingLatestVersionOrUnrecognizedStatusAsUnknown() {
        assertThat(MessageContractVersionStatusSynchronizer.stateOf(dto("1.0.0", null, false, null)))
                .isEqualTo(MessageContractVersionState.UNKNOWN);
        assertThat(MessageContractVersionStatusSynchronizer.stateOf(dto("1.0.0", "2.0.0", false, "SOMETHING_NEW")))
                .isEqualTo(MessageContractVersionState.UNKNOWN);
    }

    private static MessageContractVersionStatusDto dto(String used, String latest, boolean upToDate, String status) {
        return new MessageContractVersionStatusDto(
                "test-service", "1.0.0", "TestEvent", used, latest, "topic", "CONSUMER", upToDate, status);
    }
}
