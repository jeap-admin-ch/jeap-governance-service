package ch.admin.bit.jeap.governance.archrepo.deletion;

import ch.admin.bit.jeap.governance.archrepo.domain.RestApiRelationWithoutPact;
import ch.admin.bit.jeap.governance.archrepo.domain.RestApiRelationWithoutPactRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestApiRelationWithoutPactComponentDeletionListenerTest {

    @Mock
    private RestApiRelationWithoutPactRepository repository;

    @InjectMocks
    private RestApiRelationWithoutPactComponentDeletionListener listener;

    @Test
    void preComponentDeletion_Provider() {
        UUID systemComponentId = UUID.randomUUID();
        RestApiRelationWithoutPact entity = mock(RestApiRelationWithoutPact.class);

        when(repository.findAllByProviderSystemComponentId(systemComponentId)).thenReturn(List.of(entity));
        when(repository.findAllByConsumerSystemComponentId(systemComponentId)).thenReturn(List.of());

        listener.preComponentDeletion(systemComponentId);

        verify(repository).delete(entity);
        verify(repository).findAllByProviderSystemComponentId(systemComponentId);
        verify(repository).findAllByConsumerSystemComponentId(systemComponentId);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void preComponentDeletion_Consumer() {
        UUID systemComponentId = UUID.randomUUID();
        RestApiRelationWithoutPact entity = mock(RestApiRelationWithoutPact.class);

        when(repository.findAllByProviderSystemComponentId(systemComponentId)).thenReturn(List.of());
        when(repository.findAllByConsumerSystemComponentId(systemComponentId)).thenReturn(List.of(entity));

        listener.preComponentDeletion(systemComponentId);

        verify(repository).delete(entity);
        verify(repository).findAllByConsumerSystemComponentId(systemComponentId);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void preComponentDeletion_ProviderAndConsumer() {
        UUID systemComponentId = UUID.randomUUID();
        RestApiRelationWithoutPact entity1 = mock(RestApiRelationWithoutPact.class);
        RestApiRelationWithoutPact entity2 = mock(RestApiRelationWithoutPact.class);

        when(repository.findAllByProviderSystemComponentId(systemComponentId)).thenReturn(List.of(entity1));
        when(repository.findAllByConsumerSystemComponentId(systemComponentId)).thenReturn(List.of(entity2));

        listener.preComponentDeletion(systemComponentId);

        verify(repository).delete(entity1);
        verify(repository).delete(entity2);
        verify(repository).findAllByConsumerSystemComponentId(systemComponentId);
        verifyNoMoreInteractions(repository);
    }
}
