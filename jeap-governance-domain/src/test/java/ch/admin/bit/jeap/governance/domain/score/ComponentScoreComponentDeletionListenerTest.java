package ch.admin.bit.jeap.governance.domain.score;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ComponentScoreComponentDeletionListenerTest {

    @Mock
    private ComponentScoreRepository componentScoreRepository;
    @InjectMocks
    private ComponentScoreComponentDeletionListener componentScoreComponentDeletionListener;

    @Test
    void preComponentDeletion_deletesRelatedComponentScores() {
        long systemComponentId = 123L;

        componentScoreComponentDeletionListener.preComponentDeletion(systemComponentId);

        verify(componentScoreRepository).deleteAllBySystemComponentId(systemComponentId);
    }
}
