package ch.admin.bit.jeap.governance.web.api.management;

import ch.admin.bit.jeap.governance.dataimport.DataImportScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ManagementControllerTest {

    @Mock
    private DataImportScheduler dataImportScheduler;

    @InjectMocks
    private ManagementController managementController;

    @Test
    void triggerUpdate_ShouldInvokeDataImportScheduler_WhenJobTypeIsDataImport() {
        JobDto jobDto = new JobDto(JobType.DATA_IMPORT);

        managementController.triggerUpdate(jobDto);

        verify(dataImportScheduler).update();
    }

    @Test
    void triggerUpdate_ShouldThrowException_WhenJobDtoIsNull() {
        assertThrows(ResponseStatusException.class, () -> managementController.triggerUpdate(null));
    }

}
