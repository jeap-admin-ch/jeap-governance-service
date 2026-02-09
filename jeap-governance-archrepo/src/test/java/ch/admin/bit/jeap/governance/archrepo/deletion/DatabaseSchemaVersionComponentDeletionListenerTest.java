package ch.admin.bit.jeap.governance.archrepo.deletion;

import ch.admin.bit.jeap.governance.archrepo.domain.DatabaseSchemaVersion;
import ch.admin.bit.jeap.governance.archrepo.domain.DatabaseSchemaVersionRepository;
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
class DatabaseSchemaVersionComponentDeletionListenerTest {

    @Mock
    private DatabaseSchemaVersionRepository repository;

    @InjectMocks
    private DatabaseSchemaVersionComponentDeletionListener listener;

    @Test
    void preComponentDeletion() {
        Long systemComponentId = 42L;
        DatabaseSchemaVersion entity = mock(DatabaseSchemaVersion.class);

        when(repository.findByComponentId(systemComponentId)).thenReturn(Optional.of(entity));

        listener.preComponentDeletion(systemComponentId);

        verify(repository).delete(entity);
        verify(repository).findByComponentId(systemComponentId);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void preComponentDeletion_NoEntity() {
        Long systemComponentId = 42L;

        when(repository.findByComponentId(systemComponentId)).thenReturn(Optional.empty());

        listener.preComponentDeletion(systemComponentId);

        verify(repository).findByComponentId(systemComponentId);
        verifyNoMoreInteractions(repository);
    }

}
