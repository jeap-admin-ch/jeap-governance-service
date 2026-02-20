package ch.admin.bit.jeap.governance.domain.plugin.rule;

import ch.admin.bit.jeap.governance.domain.rule.RuleId;

public record RuleInfo(RuleId ruleId, String label, String documentationLink) {
}
