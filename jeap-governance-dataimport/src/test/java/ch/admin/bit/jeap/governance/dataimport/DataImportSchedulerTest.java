package ch.admin.bit.jeap.governance.dataimport;

import ch.admin.bit.jeap.governance.domain.scheduler.SchedulerRunRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DataImportSchedulerTest {

    private static final Executor SAME_THREAD = Runnable::run;

    private final DataImporter dataImporter = mock(DataImporter.class);
    private final SchedulerRunRepository schedulerRunRepository = mock(SchedulerRunRepository.class);
    private final LockProvider lockProvider = mock(LockProvider.class);
    private final SimpleLock lock = mock(SimpleLock.class);
    private DataImportScheduler scheduler;

    @BeforeEach
    void setUp() {
        DataImportProperties properties = new DataImportProperties();
        properties.setLockAtLeast(Duration.ofMinutes(1));
        properties.setLockAtMost(Duration.ofHours(1));
        scheduler = new DataImportScheduler(dataImporter, schedulerRunRepository, new SimpleMeterRegistry(), lockProvider, properties);
        LockAssert.TestHelper.makeAllAssertsPass(true);
        when(schedulerRunRepository.findLastRunDateTime("data-import")).thenReturn(Optional.empty());
        when(dataImporter.importData()).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        LockAssert.TestHelper.makeAllAssertsPass(false);
    }

    @Test
    void update() {
        scheduler.update();

        verify(dataImporter).importData();
        verify(schedulerRunRepository).saveLastRunDateTime(eq("data-import"), any(LocalDateTime.class));
    }

    @Test
    void update_doesNotRecordAnImportStoppedByTheShutdown() {
        when(dataImporter.importData()).thenReturn(false);

        scheduler.update();

        verify(schedulerRunRepository, never()).saveLastRunDateTime(any(), any());
    }

    @Test
    void startImport_runsTheImportUnderTheLockOfTheScheduledImport() {
        when(lockProvider.lock(any())).thenReturn(Optional.of(lock));

        boolean started = scheduler.startImport(SAME_THREAD);

        assertThat(started).isTrue();
        var config = ArgumentCaptor.forClass(LockConfiguration.class);
        verify(lockProvider).lock(config.capture());
        assertThat(config.getValue().getName()).isEqualTo("data-import");
        assertThat(config.getValue().getLockAtLeastFor()).isEqualTo(Duration.ofMinutes(1));
        assertThat(config.getValue().getLockAtMostFor()).isEqualTo(Duration.ofHours(1));
        var inOrder = inOrder(dataImporter, schedulerRunRepository, lock);
        inOrder.verify(dataImporter).importData();
        inOrder.verify(schedulerRunRepository).saveLastRunDateTime(eq("data-import"), any(LocalDateTime.class));
        inOrder.verify(lock).unlock();
    }

    @Test
    void startImport_returnsFalseWithoutImportingWhenTheLockIsHeld() {
        when(lockProvider.lock(any())).thenReturn(Optional.empty());
        Executor executor = mock(Executor.class);

        boolean started = scheduler.startImport(executor);

        assertThat(started).isFalse();
        verifyNoInteractions(executor, dataImporter);
    }

    @Test
    void startImport_releasesTheLockWhenTheImportFails() {
        when(lockProvider.lock(any())).thenReturn(Optional.of(lock));
        when(dataImporter.importData()).thenThrow(new IllegalStateException("boom"));

        scheduler.startImport(SAME_THREAD);

        verify(lock).unlock();
    }

    @Test
    void startImport_releasesTheLockWhenTheExecutorRejectsTheImport() {
        when(lockProvider.lock(any())).thenReturn(Optional.of(lock));
        Executor rejecting = _ -> {
            throw new RejectedExecutionException("full");
        };

        assertThatThrownBy(() -> scheduler.startImport(rejecting)).isInstanceOf(RejectedExecutionException.class);

        verify(lock).unlock();
        verifyNoInteractions(dataImporter);
    }

    @Test
    void init_loadsPersistedLastRunDateTime() {
        LocalDateTime persisted = LocalDateTime.of(2025, 1, 15, 10, 30);
        when(schedulerRunRepository.findLastRunDateTime("data-import")).thenReturn(Optional.of(persisted));

        scheduler.init();

        verify(schedulerRunRepository).findLastRunDateTime("data-import");
    }
}
