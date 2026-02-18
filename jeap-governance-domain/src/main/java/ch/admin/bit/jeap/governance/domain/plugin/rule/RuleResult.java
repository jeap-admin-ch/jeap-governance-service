package ch.admin.bit.jeap.governance.domain.plugin.rule;

import ch.admin.bit.jeap.governance.domain.rule.State;

public record RuleResult(State state, String stateComment) {

    public static RuleResult ok() {
        return new RuleResult(State.OK, null);
    }

    public static RuleResult failed() {
        return new RuleResult(State.FAIL, null);
    }

    public static RuleResult failed(String stateComment) {
        return new RuleResult(State.FAIL, stateComment);
    }
}
