package ch.admin.bit.jeap.governance.reactionobserver.deletion;

import ch.admin.bit.jeap.governance.reactionobserver.domain.ReactionObserverComponentLastObservationDate;
import ch.admin.bit.jeap.governance.reactionobserver.domain.ReactionObserverComponentLastObservationDateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactionObserverComponentLastObservationDateDeletionListenerTest {

    @Mock
    private ReactionObserverComponentLastObservationDateRepository repository;

    @InjectMocks
    private ReactionObserverComponentLastObservationDateDeletionListener listener;


    @Test
    void preComponentDeletion_deleteIfPresent() {
        long systemComponentId = 1L;
        var entity = mock(ReactionObserverComponentLastObservationDate.class);
        when(repository.findByComponentId(systemComponentId)).thenReturn(Optional.of(entity));

        listener.preComponentDeletion(systemComponentId);

        verify(repository).findByComponentId(systemComponentId);
        verify(repository).delete(entity);
        verifyNoMoreInteractions(repository);
    }
}
