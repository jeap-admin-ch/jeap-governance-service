package ch.admin.bit.jeap.governance.reporting;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import net.javacrumbs.shedlock.core.LockAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ReportingSchedulerTest {

    private ReportingService reportingService = mock(ReportingService.class);
    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

    private final ReportingScheduler scheduler = new ReportingScheduler(reportingService, meterRegistry);

    @BeforeEach
    void setUp() {
        LockAssert.TestHelper.makeAllAssertsPass(true);
    }

    @Test
    void generateDocumentation() {
        scheduler.generateDocumentation();

        verify(reportingService).generateSystemsReport(any(), eq(false));
        verify(reportingService).generateRulesReport(any(), eq(false));
    }

    @Test
    void generateDocumentationWithOrphanCleanup() {
        scheduler.generateDocumentationWithOrphanCleanup();

        verify(reportingService).generateSystemsReport(any(), eq(true));
        verify(reportingService).generateRulesReport(any(), eq(true));
    }

    @Test
    void createLastRunFromMetric() {
        scheduler.createLastRunFromMetric();

        var gauge = meterRegistry.find("jeap_governance_service_reporting_last_run_from").gauge();
        assertNotNull(gauge);
        assertTrue(gauge.value() >= 0);
    }
}
