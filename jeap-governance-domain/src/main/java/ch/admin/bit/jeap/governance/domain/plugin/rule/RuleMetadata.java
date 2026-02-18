package ch.admin.bit.jeap.governance.domain.plugin.rule;

import ch.admin.bit.jeap.governance.domain.rule.RuleId;
import lombok.Builder;

@Builder
public record RuleMetadata(RuleId ruleId, String label) {
}
