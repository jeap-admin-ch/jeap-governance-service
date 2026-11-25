package ch.admin.bit.jeap.governance.dataimport;

import ch.admin.bit.jeap.governance.domain.plugin.datasource.DataSourceImporter;
import ch.admin.bit.jeap.governance.domain.plugin.datasource.DataSourceUnavailableException;
import ch.admin.bit.jeap.governance.domain.scheduler.JobStopSignal;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class DataImporterTest {

    private MeterRegistry meterRegistry;
    private final JobStopSignal stopSignal = new JobStopSignal();

    @Test
    void importData_callEveryDataSourceConnectorImport() {
        DataSourceImporter connector1 = mock(DataSourceImporter.class);
        DataSourceImporter connector2 = mock(DataSourceImporter.class);

        DataImporter dataImporter = new DataImporter(List.of(connector1, connector2), meterRegistry, stopSignal);

        boolean completed = dataImporter.importData();

        verify(connector1).importData();
        verify(connector2).importData();
        assertThat(completed).isTrue();
    }

    @Test
    void importData_stopsBeforeTheNextImporterOnceTheServiceIsShuttingDown(CapturedOutput output) {
        DataSourceImporter connector1 = mock(DataSourceImporter.class);
        doAnswer(_ -> {
            stopSignal.stop();
            return null;
        }).when(connector1).importData();
        DataSourceImporter connector2 = mock(DataSourceImporter.class);

        boolean completed = new DataImporter(List.of(connector1, connector2), meterRegistry, stopSignal).importData();

        verify(connector1).importData();
        verifyNoInteractions(connector2);
        assertThat(completed).isFalse();
        assertThat(output.getAll()).containsPattern("WARN.*stopped as the service is shutting down, 1 of 2 importers not run");
    }

    @Test
    void importData_notCompletedWhenTheLastImporterIsInterruptedByTheShutdown() {
        DataSourceImporter connector = mock(DataSourceImporter.class);
        doAnswer(_ -> {
            stopSignal.stop();
            return null;
        }).when(connector).importData();

        boolean completed = new DataImporter(List.of(connector), meterRegistry, stopSignal).importData();

        assertThat(completed).isFalse();
    }

    @Test
    void importData_callEveryDataSourceConnectorImportEvenIfOneFails() {
        DataSourceImporter connector1 = mock(DataSourceImporter.class);
        doThrow(new RuntimeException("Uiuiui")).when(connector1).importData();
        DataSourceImporter connector2 = mock(DataSourceImporter.class);

        DataImporter dataImporter = new DataImporter(List.of(connector1, connector2), meterRegistry, stopSignal);

        dataImporter.importData();

        verify(connector1).importData();
        verify(connector2).importData();
    }

    @Test
    void importData_callMetricsImportEvenIfOneFails() {
        DataSourceImporter connector1 = mock(DataSourceImporter.class);
        doThrow(new RuntimeException("Uiuiui")).when(connector1).importData();
        DataSourceImporter connector2 = mock(DataSourceImporter.class);

        DataImporter dataImporter = new DataImporter(List.of(connector1, connector2), meterRegistry, stopSignal);

        dataImporter.importData();

        verify(meterRegistry).timer(eq("jeap_governance_service_data_import_duration"), anyString(), anyString(), anyString(), eq("false"));
        verify(meterRegistry).timer(eq("jeap_governance_service_data_import_duration"), anyString(), anyString(), anyString(), eq("true"));
    }

    @Test
    void importData_logsUnavailableDataSourceAsWarningAndReportsFailure(CapturedOutput output) {
        DataSourceImporter connector1 = mock(DataSourceImporter.class);
        doThrow(new DataSourceUnavailableException("source answered 503", new RuntimeException("503")))
                .when(connector1).importData();
        DataSourceImporter connector2 = mock(DataSourceImporter.class);

        DataImporter dataImporter = new DataImporter(List.of(connector1, connector2), meterRegistry, stopSignal);

        dataImporter.importData();

        verify(connector2).importData();
        verify(meterRegistry).timer(eq("jeap_governance_service_data_import_duration"), anyString(), anyString(), anyString(), eq("false"));
        assertThat(output.getAll())
                .containsPattern("WARN.*skipped, previous data retained: source answered 503")
                .doesNotContain("ERROR");
    }

    @Test
    void importData_logsOtherFailuresAsError(CapturedOutput output) {
        DataSourceImporter connector1 = mock(DataSourceImporter.class);
        doThrow(new RuntimeException("Uiuiui")).when(connector1).importData();

        new DataImporter(List.of(connector1), meterRegistry, stopSignal).importData();

        assertThat(output.getAll()).containsPattern("ERROR.*Import data with .* failed");
    }

    @BeforeEach
    void setUp() {
        Timer timer = mock(Timer.class);

        MeterRegistry.Config config = mock(MeterRegistry.Config.class);
        when(config.clock()).thenReturn(new MockClock());
        meterRegistry = mock(MeterRegistry.class);

        when(meterRegistry.timer(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(timer);
        when(meterRegistry.config()).thenReturn(config);
    }
}
