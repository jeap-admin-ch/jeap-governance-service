package ch.admin.bit.jeap.governance.domain.rule;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RuleStateComponentDeletionListenerTest {

    @Mock
    private RuleStateRepository repository;
    @InjectMocks
    private RuleStateComponentDeletionListener listener;

    @Test
    void testPreComponentDeletion() {
        long systemComponentId = 42L;

        listener.preComponentDeletion(systemComponentId);

        verify(repository).deleteAllBySystemComponentId(systemComponentId);
    }
}
