package ch.admin.bit.jeap.governance.rules;

import ch.admin.bit.jeap.governance.domain.SystemRepository;
import ch.admin.bit.jeap.governance.domain.rule.RuleConformanceRateService;
import ch.admin.bit.jeap.governance.domain.rule.RuleEvaluationResult;
import ch.admin.bit.jeap.governance.domain.scheduler.SchedulerRunRepository;
import ch.admin.bit.jeap.governance.domain.scheduler.SchedulerRunTracker;
import ch.admin.bit.jeap.governance.domain.score.ScoringService;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class ScoringScheduler {

    private final ScoringService scoringService;
    private final SystemRepository systemRepository;
    private final RuleConformanceRateService ruleConformanceRateService;
    private final SchedulerRunTracker runTracker;

    public ScoringScheduler(ScoringService scoringService, SystemRepository systemRepository,
                            RuleConformanceRateService ruleConformanceRateService,
                            SchedulerRunRepository schedulerRunRepository, MeterRegistry meterRegistry) {
        this.scoringService = scoringService;
        this.systemRepository = systemRepository;
        this.ruleConformanceRateService = ruleConformanceRateService;
        this.runTracker = new SchedulerRunTracker("scoring", "jeap_governance_service_scoring_last_run_from", schedulerRunRepository, meterRegistry);
    }

    @Scheduled(cron = "${jeap.governance.scoring.cron-expression}")
    @SchedulerLock(name = "scoring", lockAtLeastFor = "${jeap.governance.scoring.lock-at-least}", lockAtMostFor = "${jeap.governance.scoring.lock-at-most}")
    public void updateScores() {
        LockAssert.assertLocked();
        var day = LocalDate.now();
        log.info("Evaluating rules and score for the {}", day);
        List<RuleEvaluationResult> allResults = new ArrayList<>();
        Map<Long, List<RuleEvaluationResult>> resultsBySystem = new LinkedHashMap<>();

        for (long systemId : systemRepository.findAllIds()) {
            List<RuleEvaluationResult> systemResults = updateSystemScore(systemId, day);
            allResults.addAll(systemResults);
            resultsBySystem.put(systemId, systemResults);
        }

        ruleConformanceRateService.updateConformanceRates(allResults, day);
        ruleConformanceRateService.updateSystemConformanceRates(resultsBySystem, day);
        runTracker.recordRun();
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
