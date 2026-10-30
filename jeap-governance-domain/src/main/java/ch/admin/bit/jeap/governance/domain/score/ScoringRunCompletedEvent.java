package ch.admin.bit.jeap.governance.domain.score;

import java.time.OffsetDateTime;

/**
 * Published once a scoring run has completed and has been recorded.
 *
 * @param scoredAt the recorded time of the run
 */
public record ScoringRunCompletedEvent(OffsetDateTime scoredAt) {
}
