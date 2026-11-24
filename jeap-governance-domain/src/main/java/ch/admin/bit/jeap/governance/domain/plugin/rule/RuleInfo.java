package ch.admin.bit.jeap.governance.domain.plugin.rule;

import ch.admin.bit.jeap.governance.domain.rule.RuleId;

import java.time.Duration;

public record RuleInfo(RuleId ruleId, String label, Duration violationDelay) {

    public RuleInfo(RuleId ruleId, String label) {
        this(ruleId, label, Duration.ZERO);
    }
}
