package ch.admin.bit.jeap.governance.messagecontract.dataimport;

import ch.admin.bit.jeap.governance.messagecontract.connector.MessageContractConnector;
import ch.admin.bit.jeap.governance.messagecontract.connector.MessageContractVersionStatusDto;
import ch.admin.bit.jeap.governance.messagecontract.synchronize.MessageContractVersionStatusSynchronizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class MessageContractVersionImporterTest {
    private final MessageContractConnector connector = mock(MessageContractConnector.class);
    private final MessageContractVersionStatusSynchronizer synchronizer = mock(MessageContractVersionStatusSynchronizer.class);
    private final MessageContractVersionImporter importer = new MessageContractVersionImporter(connector, synchronizer);

    @Test
    void delegatesCompleteResponseToSynchronizer() {
        var status = new MessageContractVersionStatusDto(
                "test-service", "1.0.0", "TestEvent", "1.0.0", "2.0.0", "topic", "CONSUMER", false, "OUTDATED");
        when(connector.getVersionStatus()).thenReturn(List.of(status));

        importer.importData();

        verify(synchronizer).synchronize(List.of(status));
    }

    @Test
    void importsAllContractsAndWarnsAboutUnknownOnes(CapturedOutput output) {
        var outdated = new MessageContractVersionStatusDto(
                "a-service", "1.0.0", "AEvent", "1.0.0", "2.0.0", "topic", "CONSUMER", false, "OUTDATED");
        var unknown = new MessageContractVersionStatusDto(
                "billing-service", "3.2.1", "SharedEvent", "1.0.0", null, "shared", "CONSUMER", false, "UNKNOWN");
        var upToDate = new MessageContractVersionStatusDto(
                "c-service", "1.0.0", "CEvent", "1.1.0", "1.1.0", "topic", "PRODUCER", true, "UP_TO_DATE");
        when(connector.getVersionStatus()).thenReturn(List.of(outdated, unknown, upToDate));

        importer.importData();

        verify(synchronizer).synchronize(List.of(outdated, unknown, upToDate));
        assertThat(output.getAll())
                .containsPattern("WARN.*Latest message version of billing-service:3.2.1 message type SharedEvent "
                        + "\\(CONSUMER on shared\\) is unknown")
                .doesNotContain("a-service:1.0.0")
                .contains("Imported version status for 3 deployed message contract(s), 1 with unknown latest version")
                .doesNotContain("ERROR");
    }

    @Test
    void delegatesSuccessfulEmptyResponse() {
        when(connector.getVersionStatus()).thenReturn(List.of());

        importer.importData();

        verify(synchronizer).synchronize(List.of());
    }

    @Test
    void doesNotSynchronizeWhenConnectorFails() {
        when(connector.getVersionStatus()).thenThrow(new IllegalStateException("unavailable"));

        assertThatThrownBy(importer::importData).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(synchronizer);
    }
}
