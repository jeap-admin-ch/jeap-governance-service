package ch.admin.bit.jeap.governance.archrepo.synchronize;

import ch.admin.bit.jeap.governance.archrepo.connector.model.DatabaseSchemaVersionDto;
import ch.admin.bit.jeap.governance.archrepo.domain.DatabaseSchemaVersion;
import ch.admin.bit.jeap.governance.archrepo.domain.DatabaseSchemaVersionRepository;
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
class DatabaseSchemaVersionSystemSynchronizerTest {

    private static final String VERSION_1_0_0 = "1.0.0";
    private static final String VERSION_1_0_1 = "1.0.1";

    @Mock
    private SystemRepository systemRepository;
    @Mock
    private DatabaseSchemaVersionRepository databaseSchemaVersionRepository;

    @InjectMocks
    private DatabaseSchemaVersionSystemSynchronizer synchronizer;

    @Test
    void synchronizeDatabaseSchemaVersionWithArchRepo() {
        DatabaseSchemaVersionDto dto = new DatabaseSchemaVersionDto(SYSTEM_NAME_A, COMPONENT_NAME_A1, VERSION_1_0_0);
        when(systemRepository.findByName(SYSTEM_NAME_A)).thenReturn(Optional.of(SYSTEM_A));

        synchronizer.synchronizeDatabaseSchemaVersionWithArchRepo(SYSTEM_NAME_A, List.of(dto));

        ArgumentCaptor<DatabaseSchemaVersion> captor = ArgumentCaptor.forClass(DatabaseSchemaVersion.class);
        verify(databaseSchemaVersionRepository).add(captor.capture());

        DatabaseSchemaVersion addedEntity = captor.getValue();
        assertEquals(SYSTEM_COMPONENT_A1, addedEntity.getSystemComponent());
        assertEquals(VERSION_1_0_0, addedEntity.getVersion());
        assertNotNull(addedEntity.getId());
        assertNotNull(addedEntity.getCreatedAt());

        verify(databaseSchemaVersionRepository).deleteAllBySystemId(SYSTEM_A.getId());
        verify(databaseSchemaVersionRepository).add(addedEntity);
        verifyNoMoreInteractions(databaseSchemaVersionRepository);
    }

    @Test
    void synchronizeDatabaseSchemaVersionWithArchRepo_ThrowExceptionWhenSystemNotFound() {
        DatabaseSchemaVersionDto dto = new DatabaseSchemaVersionDto(SYSTEM_NAME_A, COMPONENT_NAME_A1, VERSION_1_0_0);
        when(systemRepository.findByName(SYSTEM_NAME_A)).thenReturn(Optional.empty());

        List<DatabaseSchemaVersionDto> dtos = List.of(dto);
        assertThatThrownBy(() -> synchronizer.synchronizeDatabaseSchemaVersionWithArchRepo(SYSTEM_NAME_A, dtos)).isInstanceOf(ArchRepoSynchronizeException.class);

        verifyNoInteractions(databaseSchemaVersionRepository);
    }

    @Test
    void synchronizeDatabaseSchemaVersionWithArchRepo_SeveralEntries() {
        DatabaseSchemaVersionDto dtoA1 = new DatabaseSchemaVersionDto(SYSTEM_NAME_A, COMPONENT_NAME_A1, VERSION_1_0_1);
        DatabaseSchemaVersionDto dtoANonExisting = new DatabaseSchemaVersionDto(SYSTEM_NAME_A, COMPONENT_NAME_A_NON_EXISTING, VERSION_1_0_0);
        DatabaseSchemaVersionDto dtoA2 = new DatabaseSchemaVersionDto(SYSTEM_NAME_A, COMPONENT_NAME_A2, VERSION_1_0_0);
        when(systemRepository.findByName(SYSTEM_NAME_A)).thenReturn(Optional.of(SYSTEM_A));

        synchronizer.synchronizeDatabaseSchemaVersionWithArchRepo(SYSTEM_NAME_A, List.of(dtoA1, dtoANonExisting, dtoA2));

        verify(databaseSchemaVersionRepository).deleteAllBySystemId(SYSTEM_A.getId());
        verify(databaseSchemaVersionRepository, times(2)).add(any());
        verifyNoMoreInteractions(databaseSchemaVersionRepository);
    }

}
