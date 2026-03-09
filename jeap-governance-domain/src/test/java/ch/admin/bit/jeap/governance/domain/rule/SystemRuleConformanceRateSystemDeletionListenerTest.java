package ch.admin.bit.jeap.governance.domain.rule;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SystemRuleConformanceRateSystemDeletionListenerTest {

    @Mock
    private SystemRuleConformanceRateRepository repository;
    @InjectMocks
    private SystemRuleConformanceRateSystemDeletionListener listener;

    @Test
    void preSystemDeletion_shouldDeleteAllRelatedEntities() {
        long systemId = 123L;

        listener.preSystemDeletion(systemId);

        verify(repository).deleteAllBySystemId(systemId);
    }

}
