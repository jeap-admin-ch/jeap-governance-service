package ch.admin.bit.jeap.governance.dataimport;

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

class DataImportSchedulerTest {

    private final DataImporter dataImporter = mock(DataImporter.class);
    private final SchedulerRunRepository schedulerRunRepository = mock(SchedulerRunRepository.class);
    private final DataImportScheduler scheduler = new DataImportScheduler(dataImporter, schedulerRunRepository, new SimpleMeterRegistry());

    @BeforeEach
    void setUp() {
        LockAssert.TestHelper.makeAllAssertsPass(true);
        when(schedulerRunRepository.findLastRunDateTime("data-import")).thenReturn(Optional.empty());
    }

    @Test
    void update() {
        scheduler.update();

        verify(dataImporter).importData();
        verify(schedulerRunRepository).saveLastRunDateTime(eq("data-import"), any(LocalDateTime.class));
    }

    @Test
    void init_loadsPersistedLastRunDateTime() {
        LocalDateTime persisted = LocalDateTime.of(2025, 1, 15, 10, 30);
        when(schedulerRunRepository.findLastRunDateTime("data-import")).thenReturn(Optional.of(persisted));

        scheduler.init();

        verify(schedulerRunRepository).findLastRunDateTime("data-import");
    }
}
