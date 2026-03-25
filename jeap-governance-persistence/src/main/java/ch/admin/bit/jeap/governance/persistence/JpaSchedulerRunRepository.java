package ch.admin.bit.jeap.governance.persistence;

import ch.admin.bit.jeap.governance.domain.scheduler.SchedulerRun;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaSchedulerRunRepository extends JpaRepository<SchedulerRun, String> {
}
