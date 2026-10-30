package ch.admin.bit.jeap.governance.web.api.docs.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

public record RuleStateDto(
        String ruleId,
        String name,
        RuleStateValue state,
        @Schema(description = "The first line of the rule's comment, plain text; null without a comment") String comment,
        @Schema(description = "The further lines of the rule's comment, one item each, plain text") List<String> details,
        @Schema(description = "The time the rule state last changed; comment-only changes do not update this timestamp")
        OffsetDateTime lastStatusChange) {
}
