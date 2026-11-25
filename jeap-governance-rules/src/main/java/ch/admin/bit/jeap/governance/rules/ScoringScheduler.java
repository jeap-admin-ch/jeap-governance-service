package ch.admin.bit.jeap.governance.rules;

import ch.admin.bit.jeap.governance.domain.SystemRepository;
import ch.admin.bit.jeap.governance.domain.rule.RuleConformanceRateService;
import ch.admin.bit.jeap.governance.domain.rule.RuleEvaluationResult;
import ch.admin.bit.jeap.governance.domain.scheduler.JobStopSignal;
import ch.admin.bit.jeap.governance.domain.scheduler.SchedulerRunRepository;
import ch.admin.bit.jeap.governance.domain.scheduler.SchedulerRunTracker;
import ch.admin.bit.jeap.governance.domain.score.ScoringRunCompletedEvent;
import ch.admin.bit.jeap.governance.domain.score.ScoringService;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.ClockProvider;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;

@Component
@Slf4j
public class ScoringScheduler {

    private final ScoringService scoringService;
    private final SystemRepository systemRepository;
    private final RuleConformanceRateService ruleConformanceRateService;
    private final SchedulerRunTracker runTracker;
    private final ApplicationEventPublisher eventPublisher;
    private final JobStopSignal stopSignal;
    private final LockProvider lockProvider;
    private final ScoringProperties properties;

    public ScoringScheduler(ScoringService scoringService, SystemRepository systemRepository,
                            RuleConformanceRateService ruleConformanceRateService,
                            SchedulerRunRepository schedulerRunRepository, MeterRegistry meterRegistry,
                            ApplicationEventPublisher eventPublisher, JobStopSignal stopSignal,
                            LockProvider lockProvider, ScoringProperties properties) {
        this.scoringService = scoringService;
        this.systemRepository = systemRepository;
        this.ruleConformanceRateService = ruleConformanceRateService;
        this.eventPublisher = eventPublisher;
        this.stopSignal = stopSignal;
        this.lockProvider = lockProvider;
        this.properties = properties;
        this.runTracker = new SchedulerRunTracker(SchedulerRunTracker.SCORING_JOB_NAME, "jeap_governance_service_scoring_last_run_from", schedulerRunRepository, meterRegistry);
    }

    @Scheduled(cron = "${jeap.governance.scoring.cron-expression}")
    @SchedulerLock(name = SchedulerRunTracker.SCORING_JOB_NAME, lockAtLeastFor = "${jeap.governance.scoring.lock-at-least}", lockAtMostFor = "${jeap.governance.scoring.lock-at-most}")
    public void updateScores() {
        LockAssert.assertLocked();
        runScoring();
    }

    /**
     * Starts a scoring run on the executor, holding the same lock as the scheduled run, unless that lock is held.
     *
     * @return false if the lock is held, by a run going on on any instance or left behind by one that did not end
     */
    public boolean startScoring(Executor executor) {
        Optional<SimpleLock> lock = lockProvider.lock(new LockConfiguration(
                ClockProvider.now(), SchedulerRunTracker.SCORING_JOB_NAME, properties.getLockAtMost(), properties.getLockAtLeast()));
        if (lock.isEmpty()) {
            log.info("Scoring not started: its lock is held");
            return false;
        }
        try {
            executor.execute(() -> runScoringAndUnlock(lock.get()));
        } catch (RuntimeException e) {
            lock.get().unlock();
            throw e;
        }
        return true;
    }

    private void runScoringAndUnlock(SimpleLock lock) {
        try {
            runScoring();
        } catch (RuntimeException e) {
            log.error("Scoring failed", e);
        } finally {
            lock.unlock();
        }
    }

    private void runScoring() {
        var day = LocalDate.now();
        log.info("Evaluating rules and score for the {}", day);
        List<RuleEvaluationResult> allResults = new ArrayList<>();
        Map<Long, List<RuleEvaluationResult>> resultsBySystem = new LinkedHashMap<>();

        for (long systemId : systemRepository.findAllIds()) {
            if (stopSignal.isStopRequested()) {
                // The systems scored so far keep their new scores; the next run scores all of them again
                log.warn("Scoring stopped as the service is shutting down, the run is not recorded");
                return;
            }
            List<RuleEvaluationResult> systemResults = updateSystemScore(systemId, day);
            allResults.addAll(systemResults);
            resultsBySystem.put(systemId, systemResults);
        }

        ruleConformanceRateService.updateConformanceRates(allResults, day);
        ruleConformanceRateService.updateSystemConformanceRates(resultsBySystem, day);
        LocalDateTime scoredAt = runTracker.recordRun();
        eventPublisher.publishEvent(new ScoringRunCompletedEvent(scoredAt.atZone(ZoneId.systemDefault()).toOffsetDateTime()));
    }

    private List<RuleEvaluationResult> updateSystemScore(long systemId, LocalDate day) {
        try {
            return scoringService.updateSystemScore(systemId, day);
        } catch (Exception e) {
            log.error("Failed to update score for system with id '{}'", systemId, e);
            return List.of();
        }
    }

    @PostConstruct
    public void init() {
        runTracker.init();
    }
}
