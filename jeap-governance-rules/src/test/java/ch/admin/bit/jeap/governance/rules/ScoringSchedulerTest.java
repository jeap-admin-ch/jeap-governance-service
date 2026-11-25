package ch.admin.bit.jeap.governance.rules;

import ch.admin.bit.jeap.governance.domain.SystemRepository;
import ch.admin.bit.jeap.governance.domain.rule.RuleConformanceRateService;
import ch.admin.bit.jeap.governance.domain.rule.RuleEvaluationResult;
import ch.admin.bit.jeap.governance.domain.rule.RuleId;
import ch.admin.bit.jeap.governance.domain.rule.State;
import ch.admin.bit.jeap.governance.domain.scheduler.JobStopSignal;
import ch.admin.bit.jeap.governance.domain.scheduler.SchedulerRunRepository;
import ch.admin.bit.jeap.governance.domain.score.ScoringRunCompletedEvent;
import ch.admin.bit.jeap.governance.domain.score.ScoringService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ScoringSchedulerTest {

    private final ScoringService scoringService = mock(ScoringService.class);
    private final SystemRepository systemRepository = mock(SystemRepository.class);
    private final RuleConformanceRateService ruleConformanceRateService = mock(RuleConformanceRateService.class);
    private final SchedulerRunRepository schedulerRunRepository = mock(SchedulerRunRepository.class);
    private final List<Object> publishedEvents = new ArrayList<>();
    private final JobStopSignal stopSignal = new JobStopSignal();
    private final LockProvider lockProvider = mock(LockProvider.class);
    private final SimpleLock lock = mock(SimpleLock.class);
    private ScoringScheduler scheduler;

    @BeforeEach
    void setUp() {
        ScoringProperties properties = new ScoringProperties();
        properties.setLockAtLeast(Duration.ofMinutes(1));
        properties.setLockAtMost(Duration.ofMinutes(15));
        scheduler = new ScoringScheduler(scoringService, systemRepository, ruleConformanceRateService, schedulerRunRepository,
                new SimpleMeterRegistry(), publishedEvents::add, stopSignal, lockProvider, properties);
        LockAssert.TestHelper.makeAllAssertsPass(true);
        when(schedulerRunRepository.findLastRunDateTime("scoring")).thenReturn(Optional.empty());
    }

    @AfterEach
    void tearDown() {
        LockAssert.TestHelper.makeAllAssertsPass(false);
    }

    @Test
    void updateScores_stopsBetweenSystemsOnceTheServiceIsShuttingDown_withoutRecordingTheRun() {
        when(systemRepository.findAllIds()).thenReturn(List.of(1L, 2L));
        when(scoringService.updateSystemScore(eq(1L), any())).thenAnswer(_ -> {
            stopSignal.stop();
            return List.of();
        });

        scheduler.updateScores();

        verify(scoringService).updateSystemScore(eq(1L), any());
        verify(scoringService, never()).updateSystemScore(eq(2L), any());
        verifyNoInteractions(ruleConformanceRateService);
        verify(schedulerRunRepository, never()).saveLastRunDateTime(any(), any());
        assertThat(publishedEvents).isEmpty();
    }

    @Test
    void startScoring_runsUnderTheLockOfTheScheduledRun() {
        when(lockProvider.lock(any())).thenReturn(Optional.of(lock));
        when(systemRepository.findAllIds()).thenReturn(List.of(1L));
        when(scoringService.updateSystemScore(any(Long.class), any())).thenReturn(List.of());

        boolean started = scheduler.startScoring(Runnable::run);

        assertThat(started).isTrue();
        var config = ArgumentCaptor.forClass(LockConfiguration.class);
        verify(lockProvider).lock(config.capture());
        assertThat(config.getValue().getName()).isEqualTo("scoring");
        assertThat(config.getValue().getLockAtLeastFor()).isEqualTo(Duration.ofMinutes(1));
        assertThat(config.getValue().getLockAtMostFor()).isEqualTo(Duration.ofMinutes(15));
        var inOrder = inOrder(scoringService, schedulerRunRepository, lock);
        inOrder.verify(scoringService).updateSystemScore(eq(1L), any());
        inOrder.verify(schedulerRunRepository).saveLastRunDateTime(eq("scoring"), any(LocalDateTime.class));
        inOrder.verify(lock).unlock();
        assertThat(publishedEvents).hasSize(1);
    }

    @Test
    void startScoring_returnsFalseWithoutScoringWhenTheLockIsHeld() {
        when(lockProvider.lock(any())).thenReturn(Optional.empty());
        Executor executor = mock(Executor.class);

        boolean started = scheduler.startScoring(executor);

        assertThat(started).isFalse();
        verifyNoInteractions(executor, scoringService, systemRepository);
    }

    @Test
    void startScoring_releasesTheLockWhenTheRunFails() {
        when(lockProvider.lock(any())).thenReturn(Optional.of(lock));
        when(systemRepository.findAllIds()).thenThrow(new IllegalStateException("boom"));

        scheduler.startScoring(Runnable::run);

        verify(lock).unlock();
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
        verify(ruleConformanceRateService).updateSystemConformanceRates(eq(Map.of(1L, List.of(), 2L, List.of())), any(LocalDate.class));
        verify(schedulerRunRepository).saveLastRunDateTime(eq("scoring"), any(LocalDateTime.class));
    }

    @Test
    void updateScores_callsUpdateSystemConformanceRatesWithPerSystemResults() {
        var result1 = new RuleEvaluationResult(RuleId.of("rule-1"), State.OK, null);
        var result2 = new RuleEvaluationResult(RuleId.of("rule-1"), State.FAIL, null);
        when(systemRepository.findAllIds()).thenReturn(List.of(1L, 2L));
        when(scoringService.updateSystemScore(eq(1L), any())).thenReturn(List.of(result1));
        when(scoringService.updateSystemScore(eq(2L), any())).thenReturn(List.of(result2));

        scheduler.updateScores();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<Long, List<RuleEvaluationResult>>> captor = ArgumentCaptor.forClass(Map.class);
        verify(ruleConformanceRateService).updateSystemConformanceRates(captor.capture(), any(LocalDate.class));
        Map<Long, List<RuleEvaluationResult>> resultsBySystem = captor.getValue();
        assertThat(resultsBySystem).containsKeys(1L, 2L);
        assertThat(resultsBySystem.get(1L)).containsExactly(result1);
        assertThat(resultsBySystem.get(2L)).containsExactly(result2);
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

    @Test
    void updateScores_publishesTheCompletedRunWithItsRecordedTime() {
        when(systemRepository.findAllIds()).thenReturn(List.of(1L));
        when(scoringService.updateSystemScore(any(Long.class), any())).thenReturn(List.of());

        scheduler.updateScores();

        var recordedAt = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(schedulerRunRepository).saveLastRunDateTime(eq("scoring"), recordedAt.capture());
        assertThat(publishedEvents).singleElement()
                .isInstanceOfSatisfying(ScoringRunCompletedEvent.class, event ->
                        assertThat(event.scoredAt().toLocalDateTime()).isEqualTo(recordedAt.getValue()));
    }

    @Test
    void updateScores_publishesNothingBeforeTheRunIsRecorded() {
        when(systemRepository.findAllIds()).thenReturn(List.of(1L));
        when(scoringService.updateSystemScore(any(Long.class), any())).thenAnswer(_ -> {
            assertThat(publishedEvents).isEmpty();
            return List.of();
        });
        doAnswer(_ -> {
            assertThat(publishedEvents).isEmpty();
            return null;
        }).when(schedulerRunRepository).saveLastRunDateTime(eq("scoring"), any());

        scheduler.updateScores();

        assertThat(publishedEvents).hasSize(1);
    }
}
