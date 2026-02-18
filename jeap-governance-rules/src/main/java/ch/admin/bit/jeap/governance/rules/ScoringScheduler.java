package ch.admin.bit.jeap.governance.rules;

import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemRepository;
import ch.admin.bit.jeap.governance.domain.rule.RuleConformanceRateService;
import ch.admin.bit.jeap.governance.domain.rule.RuleEvaluationResult;
import ch.admin.bit.jeap.governance.domain.score.ScoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScoringScheduler {

    private final ScoringService scoringService;
    private final SystemRepository systemRepository;
    private final RuleConformanceRateService ruleConformanceRateService;

    @Scheduled(cron = "${jeap.governance.scoring.cron-expression}")
    @SchedulerLock(name = "scoring", lockAtLeastFor = "${jeap.governance.scoring.lock-at-least}", lockAtMostFor = "${jeap.governance.scoring.lock-at-most}")
    public void updateScores() {
        LockAssert.assertLocked();
        var day = LocalDate.now();
        log.info("Evaluating rules and score for the {}", day);
        List<RuleEvaluationResult> allResults = new ArrayList<>();

        for (System system : systemRepository.findAll()) {
            allResults.addAll(updateSystemScore(system, day));
        }

        ruleConformanceRateService.updateConformanceRates(allResults, day);
    }

    private List<RuleEvaluationResult> updateSystemScore(System system, LocalDate day) {
        try {
            return scoringService.updateSystemScore(system, day);
        } catch (Exception e) {
            log.error("Failed to update score for system '{}'", system.getName(), e);
            return List.of();
        }
    }
}
