package ch.admin.bit.jeap.governance.domain.rule;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * A rule the governance service knows, active or not, as configured by the instance.
 *
 * @param explanation     the rule's explanation, see {@link ch.admin.bit.jeap.governance.domain.plugin.rule.RuleExplanation},
 *                        or null if the rule ships none
 * @param active          whether the instance activates the rule; the remaining fields are only set for an active rule
 * @param weight          the weight the scoring uses, null for an inactive rule
 * @param violationDelay  the configured violation grace period, null for an inactive rule
 * @param parameters      the configured rule parameters, empty for an inactive rule
 * @param exemptions      the component exemptions naming the rule, empty for an inactive rule
 */
public record RuleDescription(RuleId ruleId,
                              String label,
                              String explanation,
                              boolean active,
                              Integer weight,
                              Duration violationDelay,
                              Map<String, String> parameters,
                              List<RuleComponentExemption> exemptions) {
}
