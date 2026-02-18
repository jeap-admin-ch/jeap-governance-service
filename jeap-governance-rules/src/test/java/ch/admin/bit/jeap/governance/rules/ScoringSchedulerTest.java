package ch.admin.bit.jeap.governance.rules;

import ch.admin.bit.jeap.governance.domain.SystemRepository;
import ch.admin.bit.jeap.governance.domain.rule.RuleConformanceRateService;
import ch.admin.bit.jeap.governance.domain.score.ScoringService;
import net.javacrumbs.shedlock.core.LockAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ScoringSchedulerTest {

    private final ScoringService scoringService = mock(ScoringService.class);
    private final SystemRepository systemRepository = mock(SystemRepository.class);
    private final RuleConformanceRateService ruleConformanceRateService = mock(RuleConformanceRateService.class);
    private final ScoringScheduler scheduler = new ScoringScheduler(scoringService, systemRepository, ruleConformanceRateService);

    @BeforeEach
    void setUp() {
        LockAssert.TestHelper.makeAllAssertsPass(true);
    }

    @Test
    void updateScores_callsScoringServiceForEachSystem() {
        when(systemRepository.findAllIds()).thenReturn(List.of(1L, 2L));
        when(scoringService.updateSystemScore(any(Long.class), any())).thenReturn(List.of());

        scheduler.updateScores();

        var captor = ArgumentCaptor.forClass(Long.class);
        verify(scoringService, times(2)).updateSystemScore(captor.capture(), any(LocalDate.class));
        assertThat(captor.getAllValues()).containsExactly(1L, 2L);
        verify(ruleConformanceRateService).updateConformanceRates(eq(List.of()), any(LocalDate.class));
    }

    @Test
    void updateScores_oneSystemFails_continuesWithOthers() {
        when(systemRepository.findAllIds()).thenReturn(List.of(1L, 2L, 3L));
        when(scoringService.updateSystemScore(any(Long.class), any())).thenReturn(List.of());
        doThrow(new RuntimeException("DB error")).when(scoringService).updateSystemScore(eq(2L), any());

        scheduler.updateScores();

        var captor = ArgumentCaptor.forClass(Long.class);
        verify(scoringService, times(3)).updateSystemScore(captor.capture(), any(LocalDate.class));
        assertThat(captor.getAllValues()).containsExactly(1L, 2L, 3L);
    }
}
