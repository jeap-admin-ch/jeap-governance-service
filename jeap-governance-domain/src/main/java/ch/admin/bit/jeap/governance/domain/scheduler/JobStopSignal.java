package ch.admin.bit.jeap.governance.domain.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.concurrent.ExecutorConfigurationSupport;
import org.springframework.stereotype.Component;

/**
 * Tells the long-running jobs, the data import and the scoring, that the service is shutting down.
 * <p>
 * A job still running when its instance stops would keep its ShedLock lock until {@code lock-at-most} runs out, and
 * every run of the job in the meantime, scheduled or triggered, would be skipped. On shutdown, the task executors wait
 * for the running jobs only for the shutdown phase timeout, after which the database connections are closed and the
 * lock can no longer be released. This bean stops in the phase before them: a job that checks
 * {@link #isStopRequested()} between its units of work ends in time, and its lock is released.
 * <p>
 * A {@link ch.admin.bit.jeap.governance.domain.plugin.datasource.DataSourceImporter} that loops over many items should
 * check it between items, at a point where the data it has written so far is consistent, and return.
 */
@Component
@Slf4j
public class JobStopSignal implements SmartLifecycle {

    /**
     * Right before the task executor and the task scheduler, which stop in
     * {@link ExecutorConfigurationSupport#DEFAULT_PHASE}. Spring stops the highest phase first.
     */
    static final int PHASE = ExecutorConfigurationSupport.DEFAULT_PHASE + 1;

    private volatile boolean stopRequested;
    private volatile boolean running;

    /**
     * @return true once the service has started to shut down: the job should stop at its next consistent point
     */
    public boolean isStopRequested() {
        return stopRequested;
    }

    @Override
    public void start() {
        stopRequested = false;
        running = true;
    }

    @Override
    public void stop() {
        stopRequested = true;
        running = false;
        log.info("The service is shutting down: running jobs stop at their next consistent point");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return PHASE;
    }
}
