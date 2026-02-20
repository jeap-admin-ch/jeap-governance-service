package ch.admin.bit.jeap.governance.reporting.preparation;

import ch.admin.bit.jeap.governance.domain.rule.State;

import java.time.ZonedDateTime;

public record ReportingRuleState(
        String ruleId,
        String label,
        String documentationLink,
        State state,
        String stateComment,
        ZonedDateTime modifiedAt) {
}
