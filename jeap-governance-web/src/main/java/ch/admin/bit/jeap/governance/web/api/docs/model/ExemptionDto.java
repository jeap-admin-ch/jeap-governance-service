package ch.admin.bit.jeap.governance.web.api.docs.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

public record ExemptionDto(
        String id,
        String component,
        String reason,
        @Schema(description = "The day a temporary exemption ends, from which the rule applies again; null for a permanent one") LocalDate until,
        ExemptionEffect effect,
        @Schema(description = "The exemption's own parameters, sorted by name") List<ParameterDto> parameters) {
}
