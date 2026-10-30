package ch.admin.bit.jeap.governance.docservice.webhook;

import ch.admin.bit.jeap.governance.domain.score.ScoringRunCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Calls the Doc Service's webhook once a scoring run has completed, outside the scoring transaction and lock: after
 * the commit when the run was started within a transaction, as from the management API, and in any case on a thread
 * of its own. Neither a slow nor a failing call affects the scoring.
 */
@RequiredArgsConstructor
@Slf4j
class ScoringRunCompletedWebhookListener implements DisposableBean {

    private final DocServiceWebhookClient client;
    private final TaskExecutor executor;

    @TransactionalEventListener(fallbackExecution = true)
    void onScoringRunCompleted(ScoringRunCompletedEvent event) {
        try {
            executor.execute(() -> client.notifyScoringCompleted(event.scoredAt()));
        } catch (Exception e) {
            log.warn("Could not schedule the Doc Service notification of the scoring run of {}: {}",
                    event.scoredAt(), e.getMessage());
        }
    }

    /**
     * Shuts down the executor, which is not a bean of its own.
     */
    @Override
    public void destroy() throws Exception {
        if (executor instanceof DisposableBean disposable) {
            disposable.destroy();
        }
    }
}
