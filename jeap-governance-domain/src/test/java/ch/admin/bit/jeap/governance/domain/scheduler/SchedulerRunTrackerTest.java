package ch.admin.bit.jeap.governance.domain.scheduler;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SchedulerRunTrackerTest {

    private final SchedulerRunRepository repository = mock(SchedulerRunRepository.class);
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final SchedulerRunTracker tracker = new SchedulerRunTracker("test-job", "test_metric", repository, meterRegistry);

    @Test
    void init_loadsPersistedValueAndRegistersMetric() {
        LocalDateTime persisted = LocalDateTime.of(2025, 6, 15, 10, 0);
        when(repository.findLastRunDateTime("test-job")).thenReturn(Optional.of(persisted));

        tracker.init();

        verify(repository).findLastRunDateTime("test-job");
        var gauge = meterRegistry.find("test_metric").gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isNotNegative();
    }

    @Test
    void init_noPersistedValue_registersMetricWithDefaultValue() {
        when(repository.findLastRunDateTime("test-job")).thenReturn(Optional.empty());

        tracker.init();

        var gauge = meterRegistry.find("test_metric").gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isPositive();
    }

    @Test
    void recordRun_persistsAndUpdatesMetric() {
        when(repository.findLastRunDateTime("test-job")).thenReturn(Optional.empty());
        tracker.init();

        tracker.recordRun();

        verify(repository).saveLastRunDateTime(eq("test-job"), any(LocalDateTime.class));
        var gauge = meterRegistry.find("test_metric").gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isLessThanOrEqualTo(1);
    }
}
