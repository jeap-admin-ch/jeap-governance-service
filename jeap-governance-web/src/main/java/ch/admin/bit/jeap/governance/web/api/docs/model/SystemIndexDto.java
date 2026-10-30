package ch.admin.bit.jeap.governance.web.api.docs.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "Every scored system")
public record SystemIndexDto(
        @Schema(description = "The end of the last completed scoring run, null before the first one") OffsetDateTime scoredAt,
        @Schema(description = "The number of days the trends and histories cover", example = "30") int trendPeriodDays,
        List<SystemIndexEntryDto> systems) {
}
