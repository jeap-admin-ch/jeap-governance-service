package ch.admin.bit.jeap.governance.docservice.webhook;

import ch.admin.bit.jeap.governance.domain.score.ScoringRunCompletedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskRejectedException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ScoringRunCompletedWebhookListenerTest {

    private static final OffsetDateTime SCORED_AT = OffsetDateTime.parse("2026-10-29T10:47:12+01:00");

    private final DocServiceWebhookClient client = mock(DocServiceWebhookClient.class);

    @Test
    void theCallIsHandedToTheExecutor() {
        List<Runnable> scheduled = new ArrayList<>();
        ScoringRunCompletedWebhookListener listener = new ScoringRunCompletedWebhookListener(client, scheduled::add);

        listener.onScoringRunCompleted(new ScoringRunCompletedEvent(SCORED_AT));

        // Not called on the scoring's thread
        verifyNoInteractions(client);
        assertThat(scheduled).hasSize(1);
        scheduled.getFirst().run();
        verify(client).notifyScoringCompleted(SCORED_AT);
    }

    @Test
    void aRejectedCall_doesNotFailTheScoring() {
        ScoringRunCompletedWebhookListener listener = new ScoringRunCompletedWebhookListener(client, _ -> {
            throw new TaskRejectedException("queue full");
        });

        assertThatCode(() -> listener.onScoringRunCompleted(new ScoringRunCompletedEvent(SCORED_AT)))
                .doesNotThrowAnyException();
        verifyNoInteractions(client);
    }
}
