package ch.admin.bit.jeap.governance.reporting;

import ch.admin.bit.jeap.governance.reporting.confluence.ReportGenerator;
import ch.admin.bit.jeap.governance.reporting.preparation.ReportingRule;
import ch.admin.bit.jeap.governance.reporting.preparation.ReportingRulesPreparation;
import ch.admin.bit.jeap.governance.reporting.preparation.ReportingSystemScore;
import ch.admin.bit.jeap.governance.reporting.preparation.ReportingSystemsPreparation;
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
    private ReportingProperties reportingProperties;
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
        when(reportingProperties.getTrendPeriodDays()).thenReturn(TREND_PERIOD_DAYS);

        List<ReportingSystemScore> reportingSystemScores = mock(List.class);
        when(systemsPreparation.prepareAllSystemsScores(DAY.minusDays(TREND_PERIOD_DAYS), DAY)).thenReturn(reportingSystemScores);

        reportingService.generateSystemsReport(DAY, true);

        verify(reportGenerator).generateSystemsReport(reportingSystemScores, true);
    }

    @Test
    void generateRulesReport() {
        when(reportingProperties.getTrendPeriodDays()).thenReturn(TREND_PERIOD_DAYS);

        List<ReportingRule> reportingRules = mock(List.class);
        when(reportingRulesPreparation.prepareAllRules(DAY.minusDays(TREND_PERIOD_DAYS), DAY)).thenReturn(reportingRules);

        reportingService.generateRulesReport(DAY, false);

        verify(reportGenerator).generateRulesReport(reportingRules, false);
    }

    @BeforeEach
    void setUp() {
        reportingService = new ReportingService(reportingProperties, systemsPreparation, reportingRulesPreparation, reportGenerator, meterRegistry);
    }
}
