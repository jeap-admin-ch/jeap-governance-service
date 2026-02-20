package ch.admin.bit.jeap.governance.web.api.management;

import ch.admin.bit.jeap.governance.dataimport.DataImportScheduler;
import ch.admin.bit.jeap.governance.reporting.ReportingScheduler;
import ch.admin.bit.jeap.governance.rules.ScoringScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ManagementControllerTest {

    @Mock
    private DataImportScheduler dataImportScheduler;

    @Mock
    private ScoringScheduler scoringScheduler;

    @InjectMocks
    private ManagementController managementController;

    @Test
    void triggerUpdate_ShouldInvokeDataImportScheduler_WhenJobTypeIsDataImport() {
        JobDto jobDto = new JobDto(JobType.DATA_IMPORT);

        managementController.triggerUpdate(jobDto);

        verify(dataImportScheduler).update();
    }

    @Test
    void triggerUpdate_ShouldInvokeScoringScheduler_WhenJobTypeIsScoring() {
        JobDto jobDto = new JobDto(JobType.SCORING);

        managementController.triggerUpdate(jobDto);

        verify(scoringScheduler).updateScores();
    }

    @Test
    void triggerUpdate_ShouldThrowException_WhenJobDtoIsNull() {
        assertThrows(ResponseStatusException.class, () -> managementController.triggerUpdate(null));
    }

    @Test
    void triggerReporting_ShouldCallReportingScheduler() {
        ReportingScheduler reportingScheduler = mock(ReportingScheduler.class);
        managementController = new ManagementController(dataImportScheduler, scoringScheduler, Optional.of(reportingScheduler));
        JobDto jobDto = new JobDto(JobType.REPORTING);

        managementController.triggerUpdate(jobDto);

        verify(reportingScheduler).generateDocumentation();
    }

    @Test
    void triggerReportingWithOrphanCleanup_ShouldCallReportingScheduler() {
        ReportingScheduler reportingScheduler = mock(ReportingScheduler.class);
        managementController = new ManagementController(dataImportScheduler, scoringScheduler, Optional.of(reportingScheduler));
        JobDto jobDto = new JobDto(JobType.REPORTING_WITH_ORPHAN_CLEANUP);

        managementController.triggerUpdate(jobDto);

        verify(reportingScheduler).generateDocumentationWithOrphanCleanup();
    }

    @Test
    void triggerReporting_ShouldThrowException_WhenReportingSchedulerIsNotAvailable() {
        managementController = new ManagementController(dataImportScheduler, scoringScheduler, Optional.empty());
        JobDto jobDto = new JobDto(JobType.REPORTING);

        assertThrows(ResponseStatusException.class, () -> managementController.triggerUpdate(jobDto));
    }

    @Test
    void triggerReportingWithOrphanCleanup_ShouldThrowException_WhenReportingSchedulerIsNotAvailable() {
        managementController = new ManagementController(dataImportScheduler, scoringScheduler, Optional.empty());
        JobDto jobDto = new JobDto(JobType.REPORTING_WITH_ORPHAN_CLEANUP);

        assertThrows(ResponseStatusException.class, () -> managementController.triggerUpdate(jobDto));
    }
}
