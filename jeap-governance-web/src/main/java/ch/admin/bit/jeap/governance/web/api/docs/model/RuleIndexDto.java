package ch.admin.bit.jeap.governance.web.api.docs.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "Every rule the governance service knows, active or not")
public record RuleIndexDto(
        @Schema(description = "The end of the last completed scoring run, null before the first one") OffsetDateTime scoredAt,
        @Schema(description = "Sorted by rule ID") List<RuleIndexEntryDto> rules) {
}
