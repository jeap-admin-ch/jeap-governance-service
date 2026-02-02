package ch.admin.bit.jeap.governance.archrepo.synchronize;

import ch.admin.bit.jeap.governance.archrepo.connector.model.RestApiRelationWithoutPactDto;
import ch.admin.bit.jeap.governance.plugin.api.model.ComponentTechnicalIdentifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static ch.admin.bit.jeap.governance.archrepo.TestUtility.COMPONENT_NAME_A1;
import static ch.admin.bit.jeap.governance.archrepo.TestUtility.COMPONENT_NAME_A2;
import static ch.admin.bit.jeap.governance.archrepo.TestUtility.COMPONENT_NAME_B1;
import static ch.admin.bit.jeap.governance.archrepo.TestUtility.COMPONENT_NAME_B2;
import static ch.admin.bit.jeap.governance.archrepo.TestUtility.SYSTEM_NAME_A;
import static ch.admin.bit.jeap.governance.archrepo.TestUtility.SYSTEM_NAME_B;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class RestApiRelationWithoutPactSynchronizerTest {


    @Mock
    private RestApiRelationSystemComponentSynchronizer restApiRelationSystemComponentSynchronizer;

    @InjectMocks
    private RestApiRelationWithoutPactSynchronizer synchronizer;

    @Test
    void synchronizeModelWithArchRepo_oneRelation() {
        RestApiRelationWithoutPactDto relationDto = new RestApiRelationWithoutPactDto(SYSTEM_NAME_A, COMPONENT_NAME_A1, SYSTEM_NAME_B, COMPONENT_NAME_B1, "GET", "/api/resource");
        List<RestApiRelationWithoutPactDto> restApiRelationDtos = List.of(relationDto);
        synchronizer.synchronizeModelWithArchRepo(restApiRelationDtos);


        verify(restApiRelationSystemComponentSynchronizer).synchronizeRestApiRelationWithArchRepo(new ComponentTechnicalIdentifier(SYSTEM_NAME_B, COMPONENT_NAME_B1), List.of(relationDto));
        verifyNoMoreInteractions(restApiRelationSystemComponentSynchronizer);
    }

    @Test
    void synchronizeModelWithArchRepo_severalRelations() {
        RestApiRelationWithoutPactDto relationDtoA1B1 = new RestApiRelationWithoutPactDto(SYSTEM_NAME_A, COMPONENT_NAME_A1, SYSTEM_NAME_B, COMPONENT_NAME_B1, "GET", "/api/resource");
        RestApiRelationWithoutPactDto relationDtoA1B2 = new RestApiRelationWithoutPactDto(SYSTEM_NAME_A, COMPONENT_NAME_A1, SYSTEM_NAME_B, COMPONENT_NAME_B2, "GET", "/api/resource");
        RestApiRelationWithoutPactDto relationDtoA2B1 = new RestApiRelationWithoutPactDto(SYSTEM_NAME_A, COMPONENT_NAME_A2, SYSTEM_NAME_B, COMPONENT_NAME_B1, "GET", "/api/resource");
        RestApiRelationWithoutPactDto relationDtoA2B2 = new RestApiRelationWithoutPactDto(SYSTEM_NAME_A, COMPONENT_NAME_A2, SYSTEM_NAME_B, COMPONENT_NAME_B2, "GET", "/api/resource");
        List<RestApiRelationWithoutPactDto> restApiRelationDtos = List.of(relationDtoA1B1, relationDtoA1B2, relationDtoA2B1, relationDtoA2B2);

        synchronizer.synchronizeModelWithArchRepo(restApiRelationDtos);


        verify(restApiRelationSystemComponentSynchronizer).synchronizeRestApiRelationWithArchRepo(new ComponentTechnicalIdentifier(SYSTEM_NAME_B, COMPONENT_NAME_B1), List.of(relationDtoA1B1, relationDtoA2B1));
        verify(restApiRelationSystemComponentSynchronizer).synchronizeRestApiRelationWithArchRepo(new ComponentTechnicalIdentifier(SYSTEM_NAME_B, COMPONENT_NAME_B2), List.of(relationDtoA1B2, relationDtoA2B2));
        verifyNoMoreInteractions(restApiRelationSystemComponentSynchronizer);
    }

    @Test
    void synchronizeModelWithArchRepo_ProceedImportOnExceptionAndThrowAtTheEnd() {
        RestApiRelationWithoutPactDto relationDtoA1B1 = new RestApiRelationWithoutPactDto(SYSTEM_NAME_A, COMPONENT_NAME_A1, SYSTEM_NAME_B, COMPONENT_NAME_B1, "GET", "/api/resource");
        RestApiRelationWithoutPactDto relationDtoA1B2 = new RestApiRelationWithoutPactDto(SYSTEM_NAME_A, COMPONENT_NAME_A1, SYSTEM_NAME_B, COMPONENT_NAME_B2, "GET", "/api/resource");
        List<RestApiRelationWithoutPactDto> restApiRelationDtos = List.of(relationDtoA1B1, relationDtoA1B2);

        doThrow(new RuntimeException("Some exception")).when(restApiRelationSystemComponentSynchronizer).synchronizeRestApiRelationWithArchRepo(new ComponentTechnicalIdentifier(SYSTEM_NAME_B, COMPONENT_NAME_B1), List.of(relationDtoA1B1));

        assertThatThrownBy(() -> synchronizer.synchronizeModelWithArchRepo(restApiRelationDtos)).isInstanceOf(ArchRepoSynchronizeException.class);

        verify(restApiRelationSystemComponentSynchronizer).synchronizeRestApiRelationWithArchRepo(new ComponentTechnicalIdentifier(SYSTEM_NAME_B, COMPONENT_NAME_B1), List.of(relationDtoA1B1));
        verify(restApiRelationSystemComponentSynchronizer).synchronizeRestApiRelationWithArchRepo(new ComponentTechnicalIdentifier(SYSTEM_NAME_B, COMPONENT_NAME_B2), List.of(relationDtoA1B2));
        verifyNoMoreInteractions(restApiRelationSystemComponentSynchronizer);
    }


    @Test
    void synchronizeModelWithArchRepo_NoInteractionsWhenEmptyList() {
        List<RestApiRelationWithoutPactDto> restApiRelationDtos = new ArrayList<>();
        synchronizer.synchronizeModelWithArchRepo(restApiRelationDtos);

        verifyNoInteractions(restApiRelationSystemComponentSynchronizer);
    }
}
