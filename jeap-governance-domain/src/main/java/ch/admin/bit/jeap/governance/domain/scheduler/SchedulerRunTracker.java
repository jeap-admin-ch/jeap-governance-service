package ch.admin.bit.jeap.governance.domain.scheduler;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
public class SchedulerRunTracker {

    /**
     * The job name the scoring records its runs under.
     */
    public static final String SCORING_JOB_NAME = "scoring";

    private final String jobName;
    private final String metricName;
    private final SchedulerRunRepository schedulerRunRepository;
    private final MeterRegistry meterRegistry;

    private LocalDateTime lastRunDateTime = LocalDateTime.MIN;

    public SchedulerRunTracker(String jobName, String metricName, SchedulerRunRepository schedulerRunRepository, MeterRegistry meterRegistry) {
        this.jobName = jobName;
        this.metricName = metricName;
        this.schedulerRunRepository = schedulerRunRepository;
        this.meterRegistry = meterRegistry;
    }

    public void init() {
        loadLastRunDateTime();
        registerMetric();
    }

    /**
     * @return the recorded time of the run, in whole seconds so that it reads the same wherever it is passed on,
     * whatever the precision of the database
     */
    public LocalDateTime recordRun() {
        lastRunDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        schedulerRunRepository.saveLastRunDateTime(jobName, lastRunDateTime);
        return lastRunDateTime;
    }

    private void loadLastRunDateTime() {
        schedulerRunRepository.findLastRunDateTime(jobName).ifPresent(persisted -> {
            lastRunDateTime = persisted;
            log.info("Loaded persisted last {} run date/time: {}", jobName, lastRunDateTime);
        });
    }

    private void registerMetric() {
        Gauge.builder(metricName, () -> Duration.between(lastRunDateTime, LocalDateTime.now()).toMinutes())
                .baseUnit("minutes")
                .register(meterRegistry);
    }
}
