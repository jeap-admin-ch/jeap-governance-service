package ch.admin.bit.jeap.governance.reporting;

import ch.admin.bit.jeap.governance.reporting.confluence.ReportGenerator;
import ch.admin.bit.jeap.governance.reporting.preparation.ReportingRule;
import ch.admin.bit.jeap.governance.reporting.preparation.ReportingRulesPreparation;
import ch.admin.bit.jeap.governance.reporting.preparation.ReportingSystemScore;
import ch.admin.bit.jeap.governance.reporting.preparation.ReportingSystemsPreparation;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportingService {

    private final ReportingProperties reportingProperties;
    private final ReportingSystemsPreparation systemsPreparation;
    private final ReportingRulesPreparation reportingRulesPreparation;
    private final ReportGenerator reportGenerator;
    private final MeterRegistry meterRegistry;

    public void generateSystemsReport(LocalDate untilDay, boolean withOrphanCleanup) {
        timed(() -> doGenerateSystemsReport(untilDay, withOrphanCleanup), withOrphanCleanup, "jeap.governance.service.reporting.systems.overall");
    }

    public void generateRulesReport(LocalDate untilDay, boolean withOrphanCleanup) {
        timed(() -> doGenerateRulesReport(untilDay, withOrphanCleanup), withOrphanCleanup, "jeap.governance.service.reporting.rules.overall");
    }

    public void timed(Runnable runnable, boolean withOrphanCleanup, String metricsName) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            runnable.run();
        } finally {
            sample.stop(Timer.builder(metricsName)
                    .tag("withOrphanCleanup", String.valueOf(withOrphanCleanup))
                    .register(meterRegistry));
        }
    }

    public void doGenerateRulesReport(LocalDate untilDay, boolean withOrphanCleanup) {
        int trendPeriodDays = reportingProperties.getTrendPeriodDays();
        LocalDate fromDay = untilDay.minusDays(trendPeriodDays);

        log.info("Start preparing rules report for timeframe from '{}' to '{}'", fromDay, untilDay);
        List<ReportingRule> reportingRules = reportingRulesPreparation.prepareAllRules(fromDay, untilDay);
        log.info("Found {} rules with conformance rates for timeframe from '{}' to '{}'", reportingRules.size(), fromDay, untilDay);
        reportGenerator.generateRulesReport(reportingRules, withOrphanCleanup);
    }

    private void doGenerateSystemsReport(LocalDate untilDay, boolean withOrphanCleanup) {
        int trendPeriodDays = reportingProperties.getTrendPeriodDays();
        LocalDate fromDay = untilDay.minusDays(trendPeriodDays);

        log.info("Start preparing systems report for timeframe from '{}' to '{}'", fromDay, untilDay);
        List<ReportingSystemScore> allScoresPerSystem = systemsPreparation.prepareAllSystemsScores(fromDay, untilDay);
        log.info("Found {} systems with scores for timeframe from '{}' to '{}'", allScoresPerSystem.size(), fromDay, untilDay);
        reportGenerator.generateSystemsReport(allScoresPerSystem, withOrphanCleanup);
    }

}
