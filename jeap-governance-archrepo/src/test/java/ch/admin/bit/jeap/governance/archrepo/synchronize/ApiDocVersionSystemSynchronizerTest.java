package ch.admin.bit.jeap.governance.archrepo.synchronize;

import ch.admin.bit.jeap.governance.archrepo.connector.model.ApiDocVersionDto;
import ch.admin.bit.jeap.governance.archrepo.domain.ApiDocVersion;
import ch.admin.bit.jeap.governance.archrepo.domain.ApiDocVersionRepository;
import ch.admin.bit.jeap.governance.domain.SystemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static ch.admin.bit.jeap.governance.archrepo.TestUtility.COMPONENT_NAME_A1;
import static ch.admin.bit.jeap.governance.archrepo.TestUtility.COMPONENT_NAME_A2;
import static ch.admin.bit.jeap.governance.archrepo.TestUtility.COMPONENT_NAME_A_NON_EXISTING;
import static ch.admin.bit.jeap.governance.archrepo.TestUtility.SYSTEM_A;
import static ch.admin.bit.jeap.governance.archrepo.TestUtility.SYSTEM_COMPONENT_A1;
import static ch.admin.bit.jeap.governance.archrepo.TestUtility.SYSTEM_NAME_A;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiDocVersionSystemSynchronizerTest {

    private static final String VERSION_1_0_0 = "1.0.0";
    private static final String VERSION_1_0_1 = "1.0.1";

    @Mock
    private SystemRepository systemRepository;
    @Mock
    private ApiDocVersionRepository apiDocVersionRepository;

    @InjectMocks
    private ApiDocVersionSystemSynchronizer synchronizer;

    @Test
    void synchronizeApiDocVersionWithArchRepo() {
        ApiDocVersionDto dto = new ApiDocVersionDto(SYSTEM_NAME_A, COMPONENT_NAME_A1, VERSION_1_0_0);
        when(systemRepository.findByName(SYSTEM_NAME_A)).thenReturn(Optional.of(SYSTEM_A));

        synchronizer.synchronizeApiDocVersionWithArchRepo(SYSTEM_NAME_A, List.of(dto));

        ArgumentCaptor<ApiDocVersion> captor = ArgumentCaptor.forClass(ApiDocVersion.class);
        verify(apiDocVersionRepository).add(captor.capture());

        ApiDocVersion addedEntity = captor.getValue();
        assertEquals(SYSTEM_COMPONENT_A1, addedEntity.getSystemComponent());
        assertEquals(VERSION_1_0_0, addedEntity.getVersion());
        assertNotNull(addedEntity.getId());
        assertNotNull(addedEntity.getCreatedAt());

        verify(apiDocVersionRepository).deleteAllBySystemId(SYSTEM_A.getId());
        verify(apiDocVersionRepository).add(addedEntity);
        verifyNoMoreInteractions(apiDocVersionRepository);
    }

    @Test
    void synchronizeApiDocVersionWithArchRepo_ThrowExceptionWhenSystemNotFound() {
        ApiDocVersionDto dto = new ApiDocVersionDto(SYSTEM_NAME_A, COMPONENT_NAME_A1, VERSION_1_0_0);
        when(systemRepository.findByName(SYSTEM_NAME_A)).thenReturn(Optional.empty());

        List<ApiDocVersionDto> dtos = List.of(dto);
        assertThatThrownBy(() -> synchronizer.synchronizeApiDocVersionWithArchRepo(SYSTEM_NAME_A, dtos)).isInstanceOf(ArchRepoSynchronizeException.class);

        verifyNoInteractions(apiDocVersionRepository);
    }

    @Test
    void synchronizeApiDocVersionWithArchRepo_SeveralEntries() {
        ApiDocVersionDto dtoA1 = new ApiDocVersionDto(SYSTEM_NAME_A, COMPONENT_NAME_A1, VERSION_1_0_1);
        ApiDocVersionDto dtoANonExisting = new ApiDocVersionDto(SYSTEM_NAME_A, COMPONENT_NAME_A_NON_EXISTING, VERSION_1_0_0);
        ApiDocVersionDto dtoA2 = new ApiDocVersionDto(SYSTEM_NAME_A, COMPONENT_NAME_A2, VERSION_1_0_0);
        when(systemRepository.findByName(SYSTEM_NAME_A)).thenReturn(Optional.of(SYSTEM_A));


        synchronizer.synchronizeApiDocVersionWithArchRepo(SYSTEM_NAME_A, List.of(dtoA1, dtoANonExisting, dtoA2));

        verify(apiDocVersionRepository).deleteAllBySystemId(SYSTEM_A.getId());
        verify(apiDocVersionRepository, times(2)).add(any());
        verifyNoMoreInteractions(apiDocVersionRepository);
    }

}
