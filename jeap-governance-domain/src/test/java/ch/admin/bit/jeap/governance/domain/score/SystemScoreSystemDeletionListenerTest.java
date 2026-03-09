package ch.admin.bit.jeap.governance.domain.score;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SystemScoreSystemDeletionListenerTest {

    @Mock
    private SystemScoreRepository repository;
    @InjectMocks
    private SystemScoreSystemDeletionListener listener;

    @Test
    void preSystemDeletion() {
        long systemId = 42L;

        listener.preSystemDeletion(systemId);

        verify(repository).deleteAllBySystemId(systemId);
    }
}
