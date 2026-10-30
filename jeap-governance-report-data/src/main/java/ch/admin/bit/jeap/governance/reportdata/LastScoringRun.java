package ch.admin.bit.jeap.governance.reportdata;

import ch.admin.bit.jeap.governance.domain.scheduler.SchedulerRunRepository;
import ch.admin.bit.jeap.governance.domain.scheduler.SchedulerRunTracker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

/**
 * The time of the last completed scoring run, as recorded by the {@link SchedulerRunTracker} of the scoring job.
 * The run is recorded once it has completed, so a run in progress does not change it.
 */
@Component
@RequiredArgsConstructor
public class LastScoringRun {

    private final SchedulerRunRepository schedulerRunRepository;

    public Optional<OffsetDateTime> completedAt() {
        return schedulerRunRepository.findLastRunDateTime(SchedulerRunTracker.SCORING_JOB_NAME)
                .map(lastRun -> lastRun.atZone(ZoneId.systemDefault()).toOffsetDateTime());
    }
}
