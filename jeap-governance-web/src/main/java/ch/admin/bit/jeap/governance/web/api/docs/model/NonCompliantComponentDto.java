package ch.admin.bit.jeap.governance.web.api.docs.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

public record NonCompliantComponentDto(
        String system,
        String component,
        @Schema(description = "The day since which the component has been non-compliant") LocalDate since) {
}
