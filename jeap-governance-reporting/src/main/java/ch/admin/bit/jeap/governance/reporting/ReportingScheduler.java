package ch.admin.bit.jeap.governance.reporting;

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
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportingScheduler {

    private final ReportingService reportingService;
    private final MeterRegistry meterRegistry;

    private LocalDateTime lastRunDateTime = LocalDateTime.MIN;

    @Scheduled(cron = "${jeap.governance.reporting.cron-expression}")
    @SchedulerLock(name = "reporting", lockAtLeastFor = "${jeap.governance.reporting.lock-at-least}", lockAtMostFor = "${jeap.governance.reporting.lock-at-most}")
    public void generateDocumentation() {
        doGenerateDocumentation(false);
    }

    @Scheduled(cron = "${jeap.governance.reporting.orphancleanup.cron-expression}")
    @SchedulerLock(name = "reporting", lockAtLeastFor = "${jeap.governance.reporting.lock-at-least}", lockAtMostFor = "${jeap.governance.reporting.lock-at-most}")
    public void generateDocumentationWithOrphanCleanup() {
        doGenerateDocumentation(true);
    }

    private void doGenerateDocumentation(boolean withOrphanCleanup) {
        LockAssert.assertLocked();
        var day = LocalDate.now();

        generateSystemPages(day, withOrphanCleanup);
        generateRulePages(day, withOrphanCleanup);
        lastRunDateTime = LocalDateTime.now();
    }

    private void generateSystemPages(LocalDate untilDay, boolean withOrphanCleanup) {
        try {
            log.info("Generating systems pages for day '{}' with withOrphanCleanup: {}", untilDay, withOrphanCleanup);
            reportingService.generateSystemsReport(untilDay, withOrphanCleanup);
            log.info("Generating systems pages done");
        } catch (Exception e) {
            log.error("Failed to generate systems pages for day '{}'", untilDay, e);
        }
    }

    private void generateRulePages(LocalDate untilDay, boolean withOrphanCleanup) {
        try {
            log.info("Generating rules pages for day '{}' with withOrphanCleanup: {}", untilDay, withOrphanCleanup);
            reportingService.generateRulesReport(untilDay, withOrphanCleanup);
            log.info("Generating rules pages done");
        } catch (Exception e) {
            log.error("Failed to generate rules pages for day '{}'", untilDay, e);
        }
    }

    @PostConstruct
    public void createLastRunFromMetric() {
        Gauge.builder("jeap_governance_service_reporting_last_run_from", () -> calculateMinutesFromLastRunToNow(lastRunDateTime))
                .baseUnit("minutes")
                .register(meterRegistry);
    }

    private long calculateMinutesFromLastRunToNow(LocalDateTime lastRun) {
        return Duration.between(lastRun, LocalDateTime.now()).toMinutes();
    }
}
