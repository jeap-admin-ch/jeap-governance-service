package ch.admin.bit.jeap.governance.dataimport;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataImportScheduler {

    private final DataImporter dataImporter;
    private final MeterRegistry meterRegistry;

    private LocalDateTime lastRunDateTime = LocalDateTime.MIN;

    @Scheduled(cron = "${jeap.governance.dataimport.cron-expression}")
    @SchedulerLock(name = "data-import", lockAtLeastFor = "${jeap.governance.dataimport.lock-at-least}", lockAtMostFor = "${jeap.governance.dataimport.lock-at-most}")
    public void update() {
        LockAssert.assertLocked();
        dataImporter.importData();
        lastRunDateTime = LocalDateTime.now();
    }

    @PostConstruct
    private void createLastRunFromMetric() {
        Gauge.builder("jeap_governance_service_data_import_last_run_from", () -> calculateMinutesFromLastRunToNow(lastRunDateTime))
                .baseUnit("minutes")
                .register(meterRegistry);
    }

    private long calculateMinutesFromLastRunToNow(LocalDateTime lastRun) {
        return Duration.between(lastRun, LocalDateTime.now()).toMinutes();
    }
}
