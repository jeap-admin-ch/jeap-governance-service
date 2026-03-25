package ch.admin.bit.jeap.governance.domain.scheduler;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "scheduler_run")
@Getter
@Setter
@NoArgsConstructor
public class SchedulerRun {

    @Id
    @Column(name = "job_name", nullable = false)
    private String jobName;

    @Column(name = "last_run_at", nullable = false)
    private LocalDateTime lastRunAt;

    public SchedulerRun(String jobName, LocalDateTime lastRunAt) {
        this.jobName = jobName;
        this.lastRunAt = lastRunAt;
    }
}
