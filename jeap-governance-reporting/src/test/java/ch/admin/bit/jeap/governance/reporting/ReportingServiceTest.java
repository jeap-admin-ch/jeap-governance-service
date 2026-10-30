package ch.admin.bit.jeap.governance.reporting;

import ch.admin.bit.jeap.governance.reporting.confluence.ReportGenerator;
import ch.admin.bit.jeap.governance.reportdata.ReportingRule;
import ch.admin.bit.jeap.governance.reportdata.ReportingRulesPreparation;
import ch.admin.bit.jeap.governance.reportdata.ReportingSystemScore;
import ch.admin.bit.jeap.governance.reportdata.ReportingSystemsPreparation;
import ch.admin.bit.jeap.governance.reportdata.TrendPeriod;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportingServiceTest {

    private static final int TREND_PERIOD_DAYS = 7;
    private static final LocalDate DAY = LocalDate.now();

    @Mock
    private ReportingSystemsPreparation systemsPreparation;
    @Mock
    private ReportingRulesPreparation reportingRulesPreparation;
    @Mock
    private ReportGenerator reportGenerator;

    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

    private ReportingService reportingService;

    @Test
    void generateSystemsReport() {
        List<ReportingSystemScore> reportingSystemScores = mock(List.class);
        when(systemsPreparation.prepareAllSystemsScores(DAY.minusDays(TREND_PERIOD_DAYS), DAY)).thenReturn(reportingSystemScores);

        reportingService.generateSystemsReport(DAY, true);

        verify(reportGenerator).generateSystemsReport(reportingSystemScores, true);
    }

    @Test
    void generateRulesReport() {
        List<ReportingRule> reportingRules = mock(List.class);
        when(reportingRulesPreparation.prepareAllRules(DAY.minusDays(TREND_PERIOD_DAYS), DAY)).thenReturn(reportingRules);

        reportingService.generateRulesReport(DAY, false);

        verify(reportGenerator).generateRulesReport(reportingRules, false);
    }

    @BeforeEach
    void setUp() {
        reportingService = new ReportingService(new TrendPeriod(TREND_PERIOD_DAYS), systemsPreparation, reportingRulesPreparation, reportGenerator, meterRegistry);
    }
}
