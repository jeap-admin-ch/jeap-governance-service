package ch.admin.bit.jeap.governance.dataimport;

import ch.admin.bit.jeap.governance.domain.scheduler.SchedulerRunRepository;
import ch.admin.bit.jeap.governance.domain.scheduler.SchedulerRunTracker;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.ClockProvider;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.Executor;

@Component
@Slf4j
public class DataImportScheduler {

    /**
     * The name of the job: of its ShedLock lock and of its recorded runs.
     */
    public static final String JOB_NAME = "data-import";

    private final DataImporter dataImporter;
    private final SchedulerRunTracker runTracker;
    private final LockProvider lockProvider;
    private final DataImportProperties properties;

    public DataImportScheduler(DataImporter dataImporter, SchedulerRunRepository schedulerRunRepository, MeterRegistry meterRegistry,
                               LockProvider lockProvider, DataImportProperties properties) {
        this.dataImporter = dataImporter;
        this.runTracker = new SchedulerRunTracker(JOB_NAME, "jeap_governance_service_data_import_last_run_from", schedulerRunRepository, meterRegistry);
        this.lockProvider = lockProvider;
        this.properties = properties;
    }

    @Scheduled(cron = "${jeap.governance.dataimport.cron-expression}")
    @SchedulerLock(name = JOB_NAME, lockAtLeastFor = "${jeap.governance.dataimport.lock-at-least}", lockAtMostFor = "${jeap.governance.dataimport.lock-at-most}")
    public void update() {
        LockAssert.assertLocked();
        runImport();
    }

    /**
     * Starts an import on the executor, holding the same lock as the scheduled import, unless that lock is held.
     *
     * @return false if the lock is held, by an import running on any instance or left behind by one that did not end
     */
    public boolean startImport(Executor executor) {
        Optional<SimpleLock> lock = lockProvider.lock(new LockConfiguration(
                ClockProvider.now(), JOB_NAME, properties.getLockAtMost(), properties.getLockAtLeast()));
        if (lock.isEmpty()) {
            log.info("Data import not started: its lock is held");
            return false;
        }
        try {
            executor.execute(() -> runImportAndUnlock(lock.get()));
        } catch (RuntimeException e) {
            lock.get().unlock();
            throw e;
        }
        return true;
    }

    private void runImportAndUnlock(SimpleLock lock) {
        try {
            runImport();
        } catch (RuntimeException e) {
            log.error("Data import failed", e);
        } finally {
            lock.unlock();
        }
    }

    private void runImport() {
        if (dataImporter.importData()) {
            runTracker.recordRun();
        }
    }

    @PostConstruct
    public void init() {
        runTracker.init();
    }
}
