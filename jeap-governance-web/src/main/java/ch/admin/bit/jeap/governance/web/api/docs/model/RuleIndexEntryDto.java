package ch.admin.bit.jeap.governance.web.api.docs.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "One rule. An inactive rule carries no weight, share, conformance rate, trend or exemptions.")
public record RuleIndexEntryDto(
        String ruleId,
        String name,
        boolean active,
        Integer weight,
        @Schema(description = "The rule's nominal share of the total weight of all active rules in percent, one "
                              + "decimal. On a component for which other rules are disabled, the rule weighs more.",
                example = "12.5") BigDecimal share,
        @Schema(description = "The latest conformance rate in percent, null without one") Integer conformanceRate,
        Trend trend,
        @Schema(description = "The number of component exemptions naming the rule") Integer exemptionCount,
        @Schema(description = "Byte-identical to the ETag header of the rule resource",
                example = "\"sha256:41ab7c\"") String etag,
        @Schema(description = "Path of the rule resource, relative to the service root") String url) {
}
