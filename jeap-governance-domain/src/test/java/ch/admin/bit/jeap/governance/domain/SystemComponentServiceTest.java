package ch.admin.bit.jeap.governance.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SystemComponentServiceTest {

    @Mock
    private ComponentDeletionListenerDelegate deletionListenerDelegate;
    @Mock
    private SystemComponentRepository systemComponentRepository;

    @InjectMocks
    private SystemComponentService systemComponentService;

    @Test
    void testDeleteById() {
        UUID systemComponentId = UUID.randomUUID();

        systemComponentService.deleteById(systemComponentId);

        verify(deletionListenerDelegate).notifyPreComponentDeletion(systemComponentId);
        verify(systemComponentRepository).deleteById(systemComponentId);
    }
}
