package ch.admin.bit.jeap.governance.archrepo.synchronize;

import ch.admin.bit.jeap.governance.archrepo.connector.model.RestApiRelationWithoutPactDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static ch.admin.bit.jeap.governance.archrepo.TestUtility.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestApiRelationWithoutPactSynchronizerTest {


    @Mock
    private RestApiRelationSystemComponentSynchronizer restApiRelationSystemComponentSynchronizer;

    @InjectMocks
    private RestApiRelationWithoutPactSynchronizer synchronizer;

    @Test
    void synchronizeWithArchRepo_oneRelation() {
        RestApiRelationWithoutPactDto relationDto = new RestApiRelationWithoutPactDto(SYSTEM_NAME_A, COMPONENT_NAME_A1, SYSTEM_NAME_B, COMPONENT_NAME_B1, "GET", "/api/resource");
        List<RestApiRelationWithoutPactDto> restApiRelationDtos = List.of(relationDto);
        synchronizer.synchronizeWithArchRepo(restApiRelationDtos);


        verify(restApiRelationSystemComponentSynchronizer).deleteAllPreviousDataBeforeFullImport();
        verify(restApiRelationSystemComponentSynchronizer).synchronizeWithArchRepo(new ComponentTechnicalIdentifier(SYSTEM_NAME_B, COMPONENT_NAME_B1), List.of(relationDto));
        verifyNoMoreInteractions(restApiRelationSystemComponentSynchronizer);
    }

    @Test
    void synchronizeWithArchRepo_severalRelations() {
        RestApiRelationWithoutPactDto relationDtoA1B1 = new RestApiRelationWithoutPactDto(SYSTEM_NAME_A, COMPONENT_NAME_A1, SYSTEM_NAME_B, COMPONENT_NAME_B1, "GET", "/api/resource");
        RestApiRelationWithoutPactDto relationDtoA1B2 = new RestApiRelationWithoutPactDto(SYSTEM_NAME_A, COMPONENT_NAME_A1, SYSTEM_NAME_B, COMPONENT_NAME_B2, "GET", "/api/resource");
        RestApiRelationWithoutPactDto relationDtoA2B1 = new RestApiRelationWithoutPactDto(SYSTEM_NAME_A, COMPONENT_NAME_A2, SYSTEM_NAME_B, COMPONENT_NAME_B1, "GET", "/api/resource");
        RestApiRelationWithoutPactDto relationDtoA2B2 = new RestApiRelationWithoutPactDto(SYSTEM_NAME_A, COMPONENT_NAME_A2, SYSTEM_NAME_B, COMPONENT_NAME_B2, "GET", "/api/resource");
        List<RestApiRelationWithoutPactDto> restApiRelationDtos = List.of(relationDtoA1B1, relationDtoA1B2, relationDtoA2B1, relationDtoA2B2);

        synchronizer.synchronizeWithArchRepo(restApiRelationDtos);

        verify(restApiRelationSystemComponentSynchronizer).deleteAllPreviousDataBeforeFullImport();
        verify(restApiRelationSystemComponentSynchronizer).synchronizeWithArchRepo(new ComponentTechnicalIdentifier(SYSTEM_NAME_B, COMPONENT_NAME_B1), List.of(relationDtoA1B1, relationDtoA2B1));
        verify(restApiRelationSystemComponentSynchronizer).synchronizeWithArchRepo(new ComponentTechnicalIdentifier(SYSTEM_NAME_B, COMPONENT_NAME_B2), List.of(relationDtoA1B2, relationDtoA2B2));
        verifyNoMoreInteractions(restApiRelationSystemComponentSynchronizer);
    }

    @Test
    void synchronizeWithArchRepo_ProceedImportOnExceptionAndThrowAtTheEnd() {
        RestApiRelationWithoutPactDto relationDtoA1B1 = new RestApiRelationWithoutPactDto(SYSTEM_NAME_A, COMPONENT_NAME_A1, SYSTEM_NAME_B, COMPONENT_NAME_B1, "GET", "/api/resource");
        RestApiRelationWithoutPactDto relationDtoA1B2 = new RestApiRelationWithoutPactDto(SYSTEM_NAME_A, COMPONENT_NAME_A1, SYSTEM_NAME_B, COMPONENT_NAME_B2, "GET", "/api/resource");
        List<RestApiRelationWithoutPactDto> restApiRelationDtos = List.of(relationDtoA1B1, relationDtoA1B2);

        doThrow(new RuntimeException("Some exception")).when(restApiRelationSystemComponentSynchronizer).synchronizeWithArchRepo(new ComponentTechnicalIdentifier(SYSTEM_NAME_B, COMPONENT_NAME_B1), List.of(relationDtoA1B1));

        assertThatThrownBy(() -> synchronizer.synchronizeWithArchRepo(restApiRelationDtos)).isInstanceOf(ArchRepoSynchronizeException.class);

        verify(restApiRelationSystemComponentSynchronizer).deleteAllPreviousDataBeforeFullImport();
        verify(restApiRelationSystemComponentSynchronizer).synchronizeWithArchRepo(new ComponentTechnicalIdentifier(SYSTEM_NAME_B, COMPONENT_NAME_B1), List.of(relationDtoA1B1));
        verify(restApiRelationSystemComponentSynchronizer).synchronizeWithArchRepo(new ComponentTechnicalIdentifier(SYSTEM_NAME_B, COMPONENT_NAME_B2), List.of(relationDtoA1B2));
        verifyNoMoreInteractions(restApiRelationSystemComponentSynchronizer);
    }


    @Test
    void synchronizeWithArchRepo_NoInteractionsWhenEmptyList() {
        List<RestApiRelationWithoutPactDto> restApiRelationDtos = new ArrayList<>();
        synchronizer.synchronizeWithArchRepo(restApiRelationDtos);

        verifyNoInteractions(restApiRelationSystemComponentSynchronizer);
    }
}
