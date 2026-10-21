package ch.admin.bit.jeap.governance.messagecontract.persistence;

import ch.admin.bit.jeap.governance.messagecontract.domain.MessageContractVersionState;
import ch.admin.bit.jeap.governance.messagecontract.domain.MessageContractVersionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(MessageContractVersionStatusRepositoryImpl.class)
class MessageContractVersionStatusRepositoryImplTest extends PostgresTestContainerBase {
    @Autowired
    private MessageContractVersionStatusRepositoryImpl repository;

    @Test
    void persistsSnapshotForOtherRepositoryConsumers() {
        var outdated = status("test-service", MessageContractVersionState.OUTDATED);
        repository.replaceSnapshot(List.of(outdated, status("other-service", MessageContractVersionState.OUTDATED),
                status("test-service", MessageContractVersionState.UP_TO_DATE)));

        assertThat(repository.findNotUpToDateByAppName("test-service"))
                .singleElement().extracting(MessageContractVersionStatus::getMessageType).isEqualTo("TestEvent");
    }

    @Test
    void persistsUnknownStatusWithoutLatestVersion() {
        var unknown = new MessageContractVersionStatus(
                "test-service", "1.0.0", "UnknownEvent", "1.0.0", null, "topic", "CONSUMER",
                MessageContractVersionState.UNKNOWN);
        repository.replaceSnapshot(List.of(unknown, status("test-service", MessageContractVersionState.OUTDATED)));

        assertThat(repository.findNotUpToDateByAppName("test-service"))
                .extracting(MessageContractVersionStatus::getState, MessageContractVersionStatus::getLatestVersion)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(MessageContractVersionState.UNKNOWN, null),
                        org.assertj.core.groups.Tuple.tuple(MessageContractVersionState.OUTDATED, "2.0.0"));
    }

    @Test
    void successfulEmptySnapshotClearsRows() {
        repository.replaceSnapshot(List.of(status("test-service", MessageContractVersionState.OUTDATED)));

        repository.replaceSnapshot(List.of());

        assertThat(repository.findNotUpToDateByAppName("test-service")).isEmpty();
    }

    private static MessageContractVersionStatus status(String appName, MessageContractVersionState state) {
        return new MessageContractVersionStatus(
                appName, "1.0.0", "TestEvent", "1.0.0", "2.0.0", "topic", "CONSUMER", state);
    }
}
