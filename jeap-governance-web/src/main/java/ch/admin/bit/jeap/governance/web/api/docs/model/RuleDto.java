package ch.admin.bit.jeap.governance.web.api.docs.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "One rule. An inactive rule carries its name and explanation only; its other fields are null "
                      + "or empty.")
public record RuleDto(
        String ruleId,
        String name,
        boolean active,
        @Schema(description = "The rule's explanation, Markdown shipped with the rule; null if the rule has none. "
                              + "Not sanitized: render it with raw HTML shown as text.") String explanation,
        Integer weight,
        @Schema(description = "See the rules index", example = "12.5") BigDecimal share,
        @Schema(description = "The violation grace period as ISO 8601 duration", example = "PT168H") String violationDelay,
        @Schema(description = "Whether the rule has a violation grace period") boolean gracePeriodConfigured,
        @Schema(description = "The configured rule parameters, sorted by name") List<ParameterDto> parameters,
        @Schema(description = "The component exemptions naming the rule, sorted by component and ID")
        List<ExemptionDto> exemptions,
        @Schema(description = "The latest conformance rate in percent, null without one") Integer conformanceRate,
        Trend trend,
        @Schema(description = "The latest conformance rate per system, sorted by system")
        List<SystemConformanceDto> systems,
        @Schema(description = "Sorted by system and component") List<NonCompliantComponentDto> nonCompliantComponents,
        @Schema(description = "The components whose violation is still within the grace period, sorted by the end of "
                              + "the grace period; empty without a grace period")
        List<GracePeriodComponentDto> gracePeriodComponents) {
}
