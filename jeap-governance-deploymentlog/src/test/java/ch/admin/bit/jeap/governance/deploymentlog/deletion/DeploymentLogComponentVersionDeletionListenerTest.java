package ch.admin.bit.jeap.governance.deploymentlog.deletion;

import ch.admin.bit.jeap.governance.deploymentlog.domain.DeploymentLogComponentVersion;
import ch.admin.bit.jeap.governance.deploymentlog.domain.DeploymentLogComponentVersionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeploymentLogComponentVersionDeletionListenerTest {

    @Mock
    private DeploymentLogComponentVersionRepository repository;

    @InjectMocks
    private DeploymentLogComponentVersionDeletionListener listener;


    @Test
    void preComponentDeletion_deleteIfPresent() {
        Long systemComponentId = 1L;
        var entity = mock(DeploymentLogComponentVersion.class);
        when(repository.findByComponentId(systemComponentId)).thenReturn(Optional.of(entity));

        listener.preComponentDeletion(systemComponentId);

        verify(repository).findByComponentId(systemComponentId);
        verify(repository).delete(entity);
        verifyNoMoreInteractions(repository);
    }
}
