package ch.admin.bit.jeap.governance.archrepo.synchronize;

import ch.admin.bit.jeap.governance.archrepo.connector.model.ApiDocVersionDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static ch.admin.bit.jeap.governance.archrepo.TestUtility.COMPONENT_NAME_A1;
import static ch.admin.bit.jeap.governance.archrepo.TestUtility.COMPONENT_NAME_A2;
import static ch.admin.bit.jeap.governance.archrepo.TestUtility.COMPONENT_NAME_B1;
import static ch.admin.bit.jeap.governance.archrepo.TestUtility.SYSTEM_NAME_A;
import static ch.admin.bit.jeap.governance.archrepo.TestUtility.SYSTEM_NAME_B;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class ApiDocVersionSynchronizerTest {

    @Mock
    private ApiDocVersionSystemSynchronizer systemSynchronizer;

    @InjectMocks
    private ApiDocVersionSynchronizer synchronizer;

    @Test
    void synchronizeModelWithArchRepo_oneSystem() {
        ApiDocVersionDto dto1 = new ApiDocVersionDto(SYSTEM_NAME_A, COMPONENT_NAME_A1, "1.0.0");
        ApiDocVersionDto dto2 = new ApiDocVersionDto(SYSTEM_NAME_A, COMPONENT_NAME_A2, "1.2.0");
        List<ApiDocVersionDto> dtos = List.of(dto1, dto2);

        synchronizer.synchronizeModelWithArchRepo(dtos);

        verify(systemSynchronizer).synchronizeApiDocVersionWithArchRepo(SYSTEM_NAME_A, List.of(dto1, dto2));
        verifyNoMoreInteractions(systemSynchronizer);
    }

    @Test
    void synchronizeModelWithArchRepo_severalSystems() {
        ApiDocVersionDto dtoA1 = new ApiDocVersionDto(SYSTEM_NAME_A, COMPONENT_NAME_A1, "1.0.0");
        ApiDocVersionDto dtoA2 = new ApiDocVersionDto(SYSTEM_NAME_A, COMPONENT_NAME_A2, "1.2.0");
        ApiDocVersionDto dtoB1 = new ApiDocVersionDto(SYSTEM_NAME_B, COMPONENT_NAME_B1, "1.2.0");

        List<ApiDocVersionDto> dtos = List.of(dtoA1, dtoA2, dtoB1);

        synchronizer.synchronizeModelWithArchRepo(dtos);

        verify(systemSynchronizer).synchronizeApiDocVersionWithArchRepo(SYSTEM_NAME_A, List.of(dtoA1, dtoA2));
        verify(systemSynchronizer).synchronizeApiDocVersionWithArchRepo(SYSTEM_NAME_B, List.of(dtoB1));
        verifyNoMoreInteractions(systemSynchronizer);
    }

    @Test
    void synchronizeModelWithArchRepo_severalSystems_ExceptionInFirst() {
        ApiDocVersionDto dtoA1 = new ApiDocVersionDto(SYSTEM_NAME_A, COMPONENT_NAME_A1, "1.0.0");
        ApiDocVersionDto dtoA2 = new ApiDocVersionDto(SYSTEM_NAME_A, COMPONENT_NAME_A2, "1.2.0");
        ApiDocVersionDto dtoB1 = new ApiDocVersionDto(SYSTEM_NAME_B, COMPONENT_NAME_B1, "1.2.0");

        List<ApiDocVersionDto> dtos = List.of(dtoA1, dtoA2, dtoB1);

        doThrow(new RuntimeException("Something happened")).when(systemSynchronizer).synchronizeApiDocVersionWithArchRepo(SYSTEM_NAME_A, List.of(dtoA1, dtoA2));

        assertThatThrownBy(() -> synchronizer.synchronizeModelWithArchRepo(dtos)).isInstanceOf(ArchRepoSynchronizeException.class);

        verify(systemSynchronizer).synchronizeApiDocVersionWithArchRepo(SYSTEM_NAME_A, List.of(dtoA1, dtoA2));
        verify(systemSynchronizer).synchronizeApiDocVersionWithArchRepo(SYSTEM_NAME_B, List.of(dtoB1));
        verifyNoMoreInteractions(systemSynchronizer);
    }
}
