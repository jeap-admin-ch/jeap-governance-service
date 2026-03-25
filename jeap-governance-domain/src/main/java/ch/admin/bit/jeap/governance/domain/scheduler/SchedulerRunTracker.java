package ch.admin.bit.jeap.governance.domain.scheduler;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
public class SchedulerRunTracker {

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

    public void recordRun() {
        lastRunDateTime = LocalDateTime.now();
        schedulerRunRepository.saveLastRunDateTime(jobName, lastRunDateTime);
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
