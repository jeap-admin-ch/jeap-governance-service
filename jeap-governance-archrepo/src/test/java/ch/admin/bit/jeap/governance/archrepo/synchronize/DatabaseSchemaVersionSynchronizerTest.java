package ch.admin.bit.jeap.governance.archrepo.synchronize;

import ch.admin.bit.jeap.governance.archrepo.connector.model.DatabaseSchemaVersionDto;
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
class DatabaseSchemaVersionSynchronizerTest {


    @Mock
    private DatabaseSchemaVersionSystemSynchronizer systemSynchronizer;

    @InjectMocks
    private DatabaseSchemaVersionSynchronizer synchronizer;

    @Test
    void synchronizeWithArchRepo_oneSystem() {
        DatabaseSchemaVersionDto dto1 = new DatabaseSchemaVersionDto(SYSTEM_NAME_A, COMPONENT_NAME_A1, "1.0.0");
        DatabaseSchemaVersionDto dto2 = new DatabaseSchemaVersionDto(SYSTEM_NAME_A, COMPONENT_NAME_A2, "1.2.0");
        List<DatabaseSchemaVersionDto> dtos = List.of(dto1, dto2);

        synchronizer.synchronizeWithArchRepo(dtos);

        verify(systemSynchronizer).synchronizeWithArchRepo(SYSTEM_NAME_A, List.of(dto1, dto2));
        verifyNoMoreInteractions(systemSynchronizer);
    }

    @Test
    void synchronizeWithArchRepo_severalSystems() {
        DatabaseSchemaVersionDto dtoA1 = new DatabaseSchemaVersionDto(SYSTEM_NAME_A, COMPONENT_NAME_A1, "1.0.0");
        DatabaseSchemaVersionDto dtoA2 = new DatabaseSchemaVersionDto(SYSTEM_NAME_A, COMPONENT_NAME_A2, "1.2.0");
        DatabaseSchemaVersionDto dtoB1 = new DatabaseSchemaVersionDto(SYSTEM_NAME_B, COMPONENT_NAME_B1, "1.2.0");

        List<DatabaseSchemaVersionDto> dtos = List.of(dtoA1, dtoA2, dtoB1);

        synchronizer.synchronizeWithArchRepo(dtos);

        verify(systemSynchronizer).synchronizeWithArchRepo(SYSTEM_NAME_A, List.of(dtoA1, dtoA2));
        verify(systemSynchronizer).synchronizeWithArchRepo(SYSTEM_NAME_B, List.of(dtoB1));
        verifyNoMoreInteractions(systemSynchronizer);
    }

    @Test
    void synchronizeWithArchRepo_severalSystems_ExceptionInFirst() {
        DatabaseSchemaVersionDto dtoA1 = new DatabaseSchemaVersionDto(SYSTEM_NAME_A, COMPONENT_NAME_A1, "1.0.0");
        DatabaseSchemaVersionDto dtoA2 = new DatabaseSchemaVersionDto(SYSTEM_NAME_A, COMPONENT_NAME_A2, "1.2.0");
        DatabaseSchemaVersionDto dtoB1 = new DatabaseSchemaVersionDto(SYSTEM_NAME_B, COMPONENT_NAME_B1, "1.2.0");

        List<DatabaseSchemaVersionDto> dtos = List.of(dtoA1, dtoA2, dtoB1);

        doThrow(new RuntimeException("Something happened")).when(systemSynchronizer).synchronizeWithArchRepo(SYSTEM_NAME_A, List.of(dtoA1, dtoA2));

        assertThatThrownBy(() -> synchronizer.synchronizeWithArchRepo(dtos)).isInstanceOf(ArchRepoSynchronizeException.class);

        verify(systemSynchronizer).synchronizeWithArchRepo(SYSTEM_NAME_A, List.of(dtoA1, dtoA2));
        verify(systemSynchronizer).synchronizeWithArchRepo(SYSTEM_NAME_B, List.of(dtoB1));
        verifyNoMoreInteractions(systemSynchronizer);
    }
}
