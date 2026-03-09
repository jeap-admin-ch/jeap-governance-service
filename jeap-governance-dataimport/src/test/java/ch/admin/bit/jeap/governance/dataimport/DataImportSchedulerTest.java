package ch.admin.bit.jeap.governance.dataimport;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import net.javacrumbs.shedlock.core.LockAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DataImportSchedulerTest {

    private final DataImporter dataImporter = mock(DataImporter.class);
    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final DataImportScheduler scheduler = new DataImportScheduler(dataImporter, meterRegistry);

    @BeforeEach
    void setUp() {
        LockAssert.TestHelper.makeAllAssertsPass(true);
    }

    @Test
    void update() {
        scheduler.update();

        verify(dataImporter).importData();
    }

    @Test
    void createLastRunFromMetric() {
        scheduler.createLastRunFromMetric();

        var gauge = meterRegistry.find("jeap_governance_service_data_import_last_run_from").gauge();
        assertNotNull(gauge);
        assertTrue(gauge.value() >= 0);
    }

}
