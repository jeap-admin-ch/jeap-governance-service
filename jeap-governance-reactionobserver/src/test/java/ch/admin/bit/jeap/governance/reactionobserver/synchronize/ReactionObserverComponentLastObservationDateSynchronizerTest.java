package ch.admin.bit.jeap.governance.reactionobserver.synchronize;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactionObserverComponentLastObservationDateSynchronizerTest {

    @Mock
    private ReactionObserverComponentLastObservationDateOneByOneSynchronizer oneByOneSynchronizer;

    @InjectMocks
    private ReactionObserverComponentLastObservationDateSynchronizer synchronizer;

    @Test
    void synchronizeModelWithReactionObserver_twoCalls() {
        Map<String, LocalDate> map = Map.of("test1", LocalDate.of(2018, 7, 14), "test2", LocalDate.of(2022, 5, 22));

        synchronizer.synchronizeModelWithReactionObserver(map);

        verify(oneByOneSynchronizer).synchronize("test1", LocalDate.of(2018, 7, 14));
        verify(oneByOneSynchronizer).synchronize("test2", LocalDate.of(2022, 5, 22));
        verifyNoMoreInteractions(oneByOneSynchronizer);
    }

    @Test
    void synchronizeModelWithReactionObserver_severalCalls_exceptionInFirst() {
        Map<String, LocalDate> map = Map.of("test1", LocalDate.of(2018, 7, 14), "test2", LocalDate.of(2022, 5, 22));

        doThrow(new RuntimeException("Something happened")).when(oneByOneSynchronizer).synchronize("test1", LocalDate.of(2018, 7, 14));

        assertThatThrownBy(() -> synchronizer.synchronizeModelWithReactionObserver(map)).isInstanceOf(ReactionObserverSynchronizeException.class);

        verify(oneByOneSynchronizer).synchronize("test1", LocalDate.of(2018, 7, 14));
        verify(oneByOneSynchronizer).synchronize("test2", LocalDate.of(2022, 5, 22));
        verifyNoMoreInteractions(oneByOneSynchronizer);
    }
}
