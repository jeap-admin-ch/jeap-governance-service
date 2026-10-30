package ch.admin.bit.jeap.governance.web.api.docs.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ComponentDto(
        String name,
        @Schema(description = "The latest score in percent, null without a score") Integer score,
        Trend trend,
        @Schema(description = "One score per scored day of the trend period, oldest first") List<DailyScoreDto> history,
        @Schema(description = "The state of every active rule evaluated for the component, sorted by rule ID")
        List<RuleStateDto> ruleStates) {
}
