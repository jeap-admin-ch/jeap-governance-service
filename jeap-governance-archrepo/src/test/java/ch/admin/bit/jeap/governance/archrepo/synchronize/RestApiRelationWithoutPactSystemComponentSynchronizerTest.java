package ch.admin.bit.jeap.governance.archrepo.synchronize;

import ch.admin.bit.jeap.governance.archrepo.connector.model.RestApiRelationWithoutPactDto;
import ch.admin.bit.jeap.governance.archrepo.domain.RestApiRelationWithoutPact;
import ch.admin.bit.jeap.governance.archrepo.domain.RestApiRelationWithoutPactRepository;
import ch.admin.bit.jeap.governance.domain.SystemComponentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static ch.admin.bit.jeap.governance.archrepo.TestUtility.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestApiRelationWithoutPactSystemComponentSynchronizerTest {

    @Mock
    private SystemComponentRepository systemComponentRepository;
    @Mock
    private RestApiRelationWithoutPactRepository restApiRelationWithoutPactRepository;

    @InjectMocks
    private RestApiRelationSystemComponentSynchronizer synchronizer;

    @Test
    void synchronizeRestApiRelationWithArchRepo() {
        RestApiRelationWithoutPactDto relation = new RestApiRelationWithoutPactDto(SYSTEM_NAME_B, COMPONENT_NAME_B1, SYSTEM_NAME_A, COMPONENT_NAME_A1, "GET", "/api/resource");
        when(systemComponentRepository.findByName(COMPONENT_NAME_A1)).thenReturn(Optional.of(SYSTEM_COMPONENT_A1));
        when(systemComponentRepository.findByName(COMPONENT_NAME_B1)).thenReturn(Optional.of(SYSTEM_COMPONENT_B1));

        synchronizer.synchronizeRestApiRelationWithArchRepo(PROVIDER_KEY_COMPONENT_A1, List.of(relation));

        ArgumentCaptor<RestApiRelationWithoutPact> captor = ArgumentCaptor.forClass(RestApiRelationWithoutPact.class);
        verify(restApiRelationWithoutPactRepository).add(captor.capture());

        RestApiRelationWithoutPact addedRelation = captor.getValue();
        assertEquals(SYSTEM_COMPONENT_A1, addedRelation.getProviderSystemComponent());
        assertEquals(SYSTEM_COMPONENT_B1, addedRelation.getConsumerSystemComponent());
        assertEquals("GET", addedRelation.getMethod());
        assertEquals("/api/resource", addedRelation.getPath());
        assertNotNull(addedRelation.getId());

        verify(restApiRelationWithoutPactRepository).add(addedRelation);
        verify(restApiRelationWithoutPactRepository).deleteAllByProviderSystemComponentId(SYSTEM_COMPONENT_A1.getId());
        verifyNoMoreInteractions(restApiRelationWithoutPactRepository);
    }

    @Test
    void synchronizeRestApiRelationWithArchRepo_SeveralRelations() {
        RestApiRelationWithoutPactDto relation1 = new RestApiRelationWithoutPactDto(SYSTEM_NAME_B, COMPONENT_NAME_B1, SYSTEM_NAME_A, COMPONENT_NAME_A1, "GET", "/api/resource");
        RestApiRelationWithoutPactDto relationConsumerNonExisting = new RestApiRelationWithoutPactDto(SYSTEM_NAME_A, COMPONENT_NAME_A_NON_EXISTING, SYSTEM_NAME_A, COMPONENT_NAME_A1, "GET", "/api/resource");
        RestApiRelationWithoutPactDto relation2 = new RestApiRelationWithoutPactDto(SYSTEM_NAME_A, COMPONENT_NAME_A2, SYSTEM_NAME_A, COMPONENT_NAME_A1, "GET", "/api/resource");
        when(systemComponentRepository.findByName(COMPONENT_NAME_A1)).thenReturn(Optional.of(SYSTEM_COMPONENT_A1));
        when(systemComponentRepository.findByName(COMPONENT_NAME_A2)).thenReturn(Optional.of(SYSTEM_COMPONENT_A2));
        when(systemComponentRepository.findByName(COMPONENT_NAME_B1)).thenReturn(Optional.of(SYSTEM_COMPONENT_B1));
        when(systemComponentRepository.findByName(COMPONENT_NAME_A_NON_EXISTING)).thenReturn(Optional.empty());

        synchronizer.synchronizeRestApiRelationWithArchRepo(PROVIDER_KEY_COMPONENT_A1, List.of(relation1, relationConsumerNonExisting, relation2));

        verify(restApiRelationWithoutPactRepository, times(2)).add(any());
        verify(restApiRelationWithoutPactRepository).deleteAllByProviderSystemComponentId(SYSTEM_COMPONENT_A1.getId());
        verifyNoMoreInteractions(restApiRelationWithoutPactRepository);
    }

    @Test
    void synchronizeRestApiRelationWithArchRepo_DoThrowExceptionWhenFindByNameReturnsAnotherComponent() {
        when(systemComponentRepository.findByName(COMPONENT_NAME_A1)).thenReturn(Optional.of(SYSTEM_COMPONENT_A2));

        assertThatThrownBy(() -> synchronizer.synchronizeRestApiRelationWithArchRepo(PROVIDER_KEY_COMPONENT_A1, List.of()))
                .isInstanceOf(ArchRepoSynchronizeException.class);
    }

    @Test
    void synchronizeRestApiRelationWithArchRepo_DoThrowExceptionWhenFindByNameReturnsAnotherSystem() {
        when(systemComponentRepository.findByName(COMPONENT_NAME_A1)).thenReturn(Optional.of(SYSTEM_COMPONENT_B2));

        assertThatThrownBy(() -> synchronizer.synchronizeRestApiRelationWithArchRepo(PROVIDER_KEY_COMPONENT_A1, List.of()))
                .isInstanceOf(ArchRepoSynchronizeException.class);
    }

    @Test
    void synchronizeRestApiRelationWithArchRepoProviderNotFound() {
        when(systemComponentRepository.findByName(COMPONENT_NAME_A1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> synchronizer.synchronizeRestApiRelationWithArchRepo(PROVIDER_KEY_COMPONENT_A1, List.of()))
                .isInstanceOf(ArchRepoSynchronizeException.class);

        verifyNoMoreInteractions(restApiRelationWithoutPactRepository);
    }
}
