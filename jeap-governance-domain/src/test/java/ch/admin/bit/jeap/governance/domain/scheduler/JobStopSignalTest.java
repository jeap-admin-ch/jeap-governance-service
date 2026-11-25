package ch.admin.bit.jeap.governance.domain.scheduler;

import org.junit.jupiter.api.Test;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static org.assertj.core.api.Assertions.assertThat;

class JobStopSignalTest {

    private final JobStopSignal signal = new JobStopSignal();

    @Test
    void isStopRequested_falseWhileRunning() {
        signal.start();

        assertThat(signal.isStopRequested()).isFalse();
        assertThat(signal.isRunning()).isTrue();
    }

    @Test
    void isStopRequested_trueOnceStopped() {
        signal.start();

        signal.stop();

        assertThat(signal.isStopRequested()).isTrue();
        assertThat(signal.isRunning()).isFalse();
    }

    @Test
    void start_clearsAPreviousStop() {
        signal.stop();

        signal.start();

        assertThat(signal.isStopRequested()).isFalse();
    }

    @Test
    void getPhase_stopsBeforeTheTaskExecutorsAndTheScheduler() {
        // Spring stops the highest phase first
        assertThat(signal.getPhase())
                .isGreaterThan(new ThreadPoolTaskExecutor().getPhase())
                .isGreaterThan(new ThreadPoolTaskScheduler().getPhase())
                .isLessThan(SmartLifecycle.DEFAULT_PHASE);
    }
}
