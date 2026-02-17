package ch.admin.bit.jeap.governance.domain.rule;

import lombok.Builder;

@Builder
public record RuleMetadata(RuleId ruleId, String label, String documentationLink, int weight) {
}
