package ch.admin.bit.jeap.governance.dataimport;

import ch.admin.bit.jeap.governance.plugin.api.datasource.DataSourceImporter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataImporterTest {

    private MeterRegistry meterRegistry;

    @Test
    void importData_callEveryDataSourceConnectorImport() {
        DataSourceImporter connector1 = mock(DataSourceImporter.class);
        DataSourceImporter connector2 = mock(DataSourceImporter.class);

        DataImporter dataImporter = new DataImporter(
                List.of(connector1, connector2),
                meterRegistry
        );

        dataImporter.importData();

        verify(connector1).importData();
        verify(connector2).importData();
    }

    @Test
    void importData_callEveryDataSourceConnectorImportEvenIfOneFails() {
        DataSourceImporter connector1 = mock(DataSourceImporter.class);
        doThrow(new RuntimeException("Uiuiui")).when(connector1).importData();
        DataSourceImporter connector2 = mock(DataSourceImporter.class);

        DataImporter dataImporter = new DataImporter(
                List.of(connector1, connector2),
                meterRegistry
        );

        dataImporter.importData();

        verify(connector1).importData();
        verify(connector2).importData();
    }

    @Test
    void importData_callMetricsImportEvenIfOneFails() {
        DataSourceImporter connector1 = mock(DataSourceImporter.class);
        doThrow(new RuntimeException("Uiuiui")).when(connector1).importData();
        DataSourceImporter connector2 = mock(DataSourceImporter.class);

        DataImporter dataImporter = new DataImporter(
                List.of(connector1, connector2),
                meterRegistry
        );

        dataImporter.importData();

        verify(meterRegistry).timer(eq("jeap_governance_service_data_import_duration"), anyString(), anyString(), anyString(), eq("false"));
        verify(meterRegistry).timer(eq("jeap_governance_service_data_import_duration"), anyString(), anyString(), anyString(), eq("true"));
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
