package ch.admin.bit.jeap.governance.dataimport;

import ch.admin.bit.jeap.governance.domain.scheduler.SchedulerRunRepository;
import ch.admin.bit.jeap.governance.domain.scheduler.SchedulerRunTracker;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DataImportScheduler {

    private final DataImporter dataImporter;
    private final SchedulerRunTracker runTracker;

    public DataImportScheduler(DataImporter dataImporter, SchedulerRunRepository schedulerRunRepository, MeterRegistry meterRegistry) {
        this.dataImporter = dataImporter;
        this.runTracker = new SchedulerRunTracker("data-import", "jeap_governance_service_data_import_last_run_from", schedulerRunRepository, meterRegistry);
    }

    @Scheduled(cron = "${jeap.governance.dataimport.cron-expression}")
    @SchedulerLock(name = "data-import", lockAtLeastFor = "${jeap.governance.dataimport.lock-at-least}", lockAtMostFor = "${jeap.governance.dataimport.lock-at-most}")
    public void update() {
        LockAssert.assertLocked();
        dataImporter.importData();
        runTracker.recordRun();
    }

    @PostConstruct
    public void init() {
        runTracker.init();
    }
}
