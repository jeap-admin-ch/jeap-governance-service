package ch.admin.bit.jeap.governance.domain.plugin.rule;

import ch.admin.bit.jeap.governance.domain.rule.State;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public record RuleResult(State state, String stateComment) {

    public static RuleResult ok() {
        return new RuleResult(State.OK, null);
    }

    public static RuleResult ok(String stateComment) {
        return new RuleResult(State.OK, stateComment);
    }

    public static RuleResult failed() {
        return new RuleResult(State.FAIL, null);
    }

    public static RuleResult failed(String stateComment) {
        return new RuleResult(State.FAIL, stateComment);
    }

    public static RuleResult summarize(List<RuleResult> results) {
        boolean hasFailure = results.stream().anyMatch(result -> result.state().equals(State.FAIL));
        String stateComment = results.stream().map(RuleResult::stateComment).filter(Objects::nonNull).collect(Collectors.joining("; "));
        return hasFailure ? RuleResult.failed(stateComment) : RuleResult.ok(stateComment);
    }
}
