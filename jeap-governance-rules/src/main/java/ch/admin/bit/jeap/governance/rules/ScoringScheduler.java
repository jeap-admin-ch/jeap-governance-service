package ch.admin.bit.jeap.governance.rules;

import ch.admin.bit.jeap.governance.domain.SystemRepository;
import ch.admin.bit.jeap.governance.domain.score.ScoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScoringScheduler {

    private final ScoringService scoringService;
    private final SystemRepository systemRepository;

    @Scheduled(cron = "${jeap.governance.scoring.cron-expression}")
    @SchedulerLock(name = "scoring", lockAtLeastFor = "${jeap.governance.scoring.lock-at-least}", lockAtMostFor = "${jeap.governance.scoring.lock-at-most}")
    public void updateScores() {
        LockAssert.assertLocked();
        systemRepository.findAll().forEach(this::updateSystemScore);
    }

    private void updateSystemScore(ch.admin.bit.jeap.governance.domain.System system) {
        try {
            scoringService.updateSystemScore(system);
        } catch (Exception e) {
            log.error("Failed to update score for system '{}'", system.getName(), e);
        }
    }
}
