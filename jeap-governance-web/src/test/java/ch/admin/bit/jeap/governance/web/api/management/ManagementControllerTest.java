package ch.admin.bit.jeap.governance.web.api.management;

import ch.admin.bit.jeap.governance.dataimport.DataImportScheduler;
import ch.admin.bit.jeap.governance.rules.ScoringScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManagementControllerTest {

    @Mock
    private DataImportScheduler dataImportScheduler;

    @Mock
    private ScoringScheduler scoringScheduler;

    private ManagementController managementController() {
        return new ManagementController(dataImportScheduler, scoringScheduler, new AsyncJobRunner());
    }

    @Test
    void triggerUpdate_startsTheDataImport() {
        when(dataImportScheduler.startImport(any())).thenReturn(true);

        assertThatNoException().isThrownBy(() -> managementController().triggerUpdate(new JobDto(JobType.DATA_IMPORT)));

        verifyNoInteractions(scoringScheduler);
    }

    @Test
    void triggerUpdate_startsTheScoring() {
        when(scoringScheduler.startScoring(any())).thenReturn(true);

        assertThatNoException().isThrownBy(() -> managementController().triggerUpdate(new JobDto(JobType.SCORING)));

        verifyNoInteractions(dataImportScheduler);
    }

    @Test
    void triggerUpdate_answersConflictWhenTheDataImportLockIsHeld() {
        when(dataImportScheduler.startImport(any())).thenReturn(false);

        assertThatThrownBy(() -> managementController().triggerUpdate(new JobDto(JobType.DATA_IMPORT)))
                .isInstanceOfSatisfying(ResponseStatusException.class, e ->
                        assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void triggerUpdate_answersConflictWhenTheScoringLockIsHeld() {
        when(scoringScheduler.startScoring(any())).thenReturn(false);

        assertThatThrownBy(() -> managementController().triggerUpdate(new JobDto(JobType.SCORING)))
                .isInstanceOfSatisfying(ResponseStatusException.class, e ->
                        assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void triggerUpdate_answersBadRequestWithoutAJob() {
        assertThatThrownBy(() -> managementController().triggerUpdate(null))
                .isInstanceOfSatisfying(ResponseStatusException.class, e ->
                        assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void triggerUpdate_answersBadRequestWithoutAJobType() {
        assertThatThrownBy(() -> managementController().triggerUpdate(new JobDto(null)))
                .isInstanceOfSatisfying(ResponseStatusException.class, e ->
                        assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(dataImportScheduler, scoringScheduler);
    }
}
