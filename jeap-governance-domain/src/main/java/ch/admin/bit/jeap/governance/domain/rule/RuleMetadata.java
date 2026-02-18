package ch.admin.bit.jeap.governance.domain.rule;

import lombok.Builder;

@Builder
/** Descriptive metadata for a rule, including its identifier, display label, documentation link, and scoring weight. */
public record RuleMetadata(RuleId ruleId, String label, String documentationLink, int weight) {
}
