package ch.admin.bit.jeap.governance.domain.rule;

import java.time.LocalDate;

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
