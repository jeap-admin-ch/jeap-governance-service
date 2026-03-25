package ch.admin.bit.jeap.governance.persistence;

import ch.admin.bit.jeap.governance.domain.scheduler.SchedulerRun;
import ch.admin.bit.jeap.governance.domain.scheduler.SchedulerRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SchedulerRunRepositoryImpl implements SchedulerRunRepository {

    private final JpaSchedulerRunRepository jpaSchedulerRunRepository;

    @Override
    public Optional<LocalDateTime> findLastRunDateTime(String jobName) {
        return jpaSchedulerRunRepository.findById(jobName)
                .map(SchedulerRun::getLastRunAt);
    }

    @Override
    public void saveLastRunDateTime(String jobName, LocalDateTime lastRunDateTime) {
        SchedulerRun run = jpaSchedulerRunRepository.findById(jobName)
                .orElse(new SchedulerRun(jobName, lastRunDateTime));
        run.setLastRunAt(lastRunDateTime);
        jpaSchedulerRunRepository.save(run);
    }
}
