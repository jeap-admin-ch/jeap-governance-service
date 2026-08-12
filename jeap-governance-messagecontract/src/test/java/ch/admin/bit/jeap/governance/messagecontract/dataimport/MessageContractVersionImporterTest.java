package ch.admin.bit.jeap.governance.messagecontract.dataimport;

import ch.admin.bit.jeap.governance.messagecontract.connector.MessageContractConnector;
import ch.admin.bit.jeap.governance.messagecontract.connector.MessageContractVersionStatusDto;
import ch.admin.bit.jeap.governance.messagecontract.synchronize.MessageContractVersionStatusSynchronizer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

class MessageContractVersionImporterTest {
    private final MessageContractConnector connector = mock(MessageContractConnector.class);
    private final MessageContractVersionStatusSynchronizer synchronizer = mock(MessageContractVersionStatusSynchronizer.class);
    private final MessageContractVersionImporter importer = new MessageContractVersionImporter(connector, synchronizer);

    @Test
    void delegatesCompleteResponseToSynchronizer() {
        var status = new MessageContractVersionStatusDto(
                "test-service", "1.0.0", "TestEvent", "1.0.0", "2.0.0", "topic", "CONSUMER", false);
        when(connector.getVersionStatus()).thenReturn(List.of(status));

        importer.importData();

        verify(synchronizer).synchronize(List.of(status));
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

        org.assertj.core.api.Assertions.assertThatThrownBy(importer::importData).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(synchronizer);
    }
}
