package ch.admin.bit.jeap.governance.web.api.docs.model;

import io.swagger.v3.oas.annotations.media.Schema;

public record SystemIndexEntryDto(
        String name,
        @Schema(description = "The latest score in percent, null without a score") Integer score,
        Trend trend,
        @Schema(description = "Byte-identical to the ETag header of the system resource",
                example = "\"sha256:41ab7c\"") String etag,
        @Schema(description = "Path of the system resource, with the service's context path and without scheme "
                              + "and host") String url) {
}
