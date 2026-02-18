package ch.admin.bit.jeap.governance.domain.rule;

import java.time.LocalDate;

/**
 * Indicates whether a rule is active or exempted for a given component.
 * Exepmtions can be indefinite ({@link #EXEMPTED} or until a certain date ({@link #EXEMPTED_UNTIL}),
 * at which the rules becomse {@link #ACTIVE} again.
 */
public enum RuleActivationState {

    ACTIVE,
    EXEMPTED,
    EXEMPTED_UNTIL;

    public static RuleActivationState stateForRuleWithOptionalExemption(LocalDate today, LocalDate exemptedUntil) {
        if (exemptedUntil == null) {
            return RuleActivationState.EXEMPTED;
        }
        if (exemptedUntil.isAfter(today)) {
            return RuleActivationState.EXEMPTED_UNTIL;
        }
        return RuleActivationState.ACTIVE;
    }
}
