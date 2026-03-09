package ch.admin.bit.jeap.governance.reactionobserver.dataimport;

import ch.admin.bit.jeap.governance.reactionobserver.connector.ReactionObserverConnector;
import ch.admin.bit.jeap.governance.reactionobserver.synchronize.ReactionObserverComponentLastObservationDateSynchronizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactionObserverComponentLastObservationDateImporterTest {

    @Mock
    private ReactionObserverConnector connector;
    @Mock
    private ReactionObserverComponentLastObservationDateSynchronizer synchronizer;

    @InjectMocks
    private ReactionObserverComponentLastObservationDateImporter importer;


    @Test
    void importData() {
        Map<String, LocalDate> map = Map.of("test", LocalDate.now());

        when(connector.getAllComponentLastObservationDates()).thenReturn(map);

        importer.importData();

        verify(synchronizer).synchronizeModelWithReactionObserver(map);
        verifyNoMoreInteractions(synchronizer);

        verify(connector).getAllComponentLastObservationDates();
        verifyNoMoreInteractions(connector);
    }
}
