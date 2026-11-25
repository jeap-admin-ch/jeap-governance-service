package ch.admin.bit.jeap.governance.rules;

import ch.admin.bit.jeap.governance.domain.SystemRepository;
import ch.admin.bit.jeap.governance.domain.rule.RuleConformanceRateService;
import ch.admin.bit.jeap.governance.domain.scheduler.JobStopSignal;
import ch.admin.bit.jeap.governance.domain.scheduler.SchedulerRunRepository;
import ch.admin.bit.jeap.governance.domain.score.ScoringService;
import ch.admin.bit.jeap.governance.reportdata.LastScoringRun;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The report data shows the time of the last <em>completed</em> scoring run, read from what the scoring records.
 */
class LastScoringRunTest {

    private static final LocalDateTime PREVIOUS_RUN = LocalDateTime.of(2026, 10, 29, 10, 0);

    private final InMemorySchedulerRunRepository schedulerRunRepository = new InMemorySchedulerRunRepository();
    private final ScoringService scoringService = mock(ScoringService.class);
    private final SystemRepository systemRepository = mock(SystemRepository.class);
    private final LastScoringRun lastScoringRun = new LastScoringRun(schedulerRunRepository);
    private final JobStopSignal stopSignal = new JobStopSignal();
    private ScoringScheduler scoringScheduler;

    @BeforeEach
    void setUp() {
        LockAssert.TestHelper.makeAllAssertsPass(true);
        schedulerRunRepository.saveLastRunDateTime("scoring", PREVIOUS_RUN);
        scoringScheduler = new ScoringScheduler(scoringService, systemRepository, mock(RuleConformanceRateService.class),
                schedulerRunRepository, new SimpleMeterRegistry(), _ -> {
        }, stopSignal, mock(LockProvider.class), new ScoringProperties());
        scoringScheduler.init();
        when(systemRepository.findAllIds()).thenReturn(List.of(1L));
    }

    @Test
    void aRunInProgress_doesNotChangeIt() {
        List<Optional<OffsetDateTime>> seenWhileScoring = new ArrayList<>();
        when(scoringService.updateSystemScore(anyLong(), any())).thenAnswer(_ -> {
            seenWhileScoring.add(lastScoringRun.completedAt());
            return List.of();
        });

        scoringScheduler.updateScores();

        assertThat(seenWhileScoring).containsExactly(Optional.of(atSystemZone(PREVIOUS_RUN)));
    }

    @Test
    void aCompletedRun_changesIt() {
        when(scoringService.updateSystemScore(anyLong(), any())).thenReturn(List.of());

        scoringScheduler.updateScores();

        assertThat(lastScoringRun.completedAt()).hasValueSatisfying(completedAt ->
                assertThat(completedAt).isAfter(atSystemZone(PREVIOUS_RUN)));
        assertThat(lastScoringRun.completedAt())
                .contains(atSystemZone(schedulerRunRepository.findLastRunDateTime("scoring").orElseThrow()));
    }

    @Test
    void aRunStoppedByTheShutdown_doesNotChangeIt() {
        when(systemRepository.findAllIds()).thenReturn(List.of(1L, 2L));
        when(scoringService.updateSystemScore(anyLong(), any())).thenAnswer(_ -> {
            stopSignal.stop();
            return List.of();
        });

        scoringScheduler.updateScores();

        assertThat(lastScoringRun.completedAt()).contains(atSystemZone(PREVIOUS_RUN));
    }

    @Test
    void withoutAnyRun_isEmpty() {
        assertThat(new LastScoringRun(new InMemorySchedulerRunRepository()).completedAt()).isEmpty();
    }

    private static OffsetDateTime atSystemZone(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    private static class InMemorySchedulerRunRepository implements SchedulerRunRepository {
        private final Map<String, LocalDateTime> lastRuns = new HashMap<>();

        @Override
        public Optional<LocalDateTime> findLastRunDateTime(String jobName) {
            return Optional.ofNullable(lastRuns.get(jobName));
        }

        @Override
        public void saveLastRunDateTime(String jobName, LocalDateTime lastRunDateTime) {
            lastRuns.put(jobName, lastRunDateTime);
        }
    }
}
