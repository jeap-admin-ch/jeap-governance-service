package ch.admin.bit.jeap.governance.reporting;

import ch.admin.bit.jeap.governance.domain.scheduler.SchedulerRunRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import net.javacrumbs.shedlock.core.LockAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ReportingSchedulerTest {

    private final ReportingService reportingService = mock(ReportingService.class);
    private final SchedulerRunRepository schedulerRunRepository = mock(SchedulerRunRepository.class);
    private final ReportingScheduler scheduler = new ReportingScheduler(reportingService, schedulerRunRepository, new SimpleMeterRegistry());

    @BeforeEach
    void setUp() {
        LockAssert.TestHelper.makeAllAssertsPass(true);
        when(schedulerRunRepository.findLastRunDateTime("reporting")).thenReturn(Optional.empty());
    }

    @Test
    void generateDocumentation() {
        scheduler.generateDocumentation();

        verify(reportingService).generateSystemsReport(any(), eq(false));
        verify(reportingService).generateRulesReport(any(), eq(false));
        verify(schedulerRunRepository).saveLastRunDateTime(eq("reporting"), any(LocalDateTime.class));
    }

    @Test
    void generateDocumentationWithOrphanCleanup() {
        scheduler.generateDocumentationWithOrphanCleanup();

        verify(reportingService).generateSystemsReport(any(), eq(true));
        verify(reportingService).generateRulesReport(any(), eq(true));
        verify(schedulerRunRepository).saveLastRunDateTime(eq("reporting"), any(LocalDateTime.class));
    }
}
