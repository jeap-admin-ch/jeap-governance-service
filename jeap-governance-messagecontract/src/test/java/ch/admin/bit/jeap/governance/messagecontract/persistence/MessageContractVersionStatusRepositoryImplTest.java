package ch.admin.bit.jeap.governance.messagecontract.persistence;

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
        var outdated = status("test-service", false);
        repository.replaceSnapshot(List.of(outdated, status("other-service", false), status("test-service", true)));

        assertThat(repository.findOutdatedByAppName("test-service"))
                .singleElement().extracting(MessageContractVersionStatus::getMessageType).isEqualTo("TestEvent");
    }

    @Test
    void successfulEmptySnapshotClearsRows() {
        repository.replaceSnapshot(List.of(status("test-service", false)));

        repository.replaceSnapshot(List.of());

        assertThat(repository.findOutdatedByAppName("test-service")).isEmpty();
    }

    private static MessageContractVersionStatus status(String appName, boolean upToDate) {
        return new MessageContractVersionStatus(
                appName, "1.0.0", "TestEvent", "1.0.0", "2.0.0", "topic", "CONSUMER", upToDate);
    }
}
