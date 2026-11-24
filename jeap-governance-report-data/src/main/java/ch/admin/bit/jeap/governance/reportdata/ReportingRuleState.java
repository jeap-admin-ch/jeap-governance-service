package ch.admin.bit.jeap.governance.reportdata;

import ch.admin.bit.jeap.governance.domain.rule.State;

import java.time.ZonedDateTime;

public record ReportingRuleState(
        String ruleId,
        String label,
        State state,
        String stateComment,
        ZonedDateTime modifiedAt) {
}
