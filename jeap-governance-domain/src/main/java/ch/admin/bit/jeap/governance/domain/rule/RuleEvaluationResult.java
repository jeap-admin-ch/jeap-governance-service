package ch.admin.bit.jeap.governance.domain.rule;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import org.springframework.util.StringUtils;

/**
 * The outcome of evaluating a rule against a component, carrying the resulting state and an optional comment for failed rules.
 */
public record RuleEvaluationResult(RuleId ruleId, State state, String stateComment) {

    public static RuleEvaluationResult ok(RuleId ruleId) {
        return new RuleEvaluationResult(ruleId, State.OK, null);
    }

    public static RuleEvaluationResult failed(RuleId ruleId) {
        return new RuleEvaluationResult(ruleId, State.FAIL, null);
    }

    public static RuleEvaluationResult failed(RuleId ruleId, String stateComment) {
        return new RuleEvaluationResult(ruleId, State.FAIL, stateComment);
    }

    static RuleEvaluationResult exempted(RuleId ruleId) {
        return new RuleEvaluationResult(ruleId, State.DISABLED, null);
    }

    static RuleEvaluationResult exemptedUntil(RuleId ruleId) {
        return new RuleEvaluationResult(ruleId, State.PAUSED, null);
    }

    public boolean isOk() {
        return state == State.OK;
    }

    RuleState toRuleState(SystemComponent systemComponent) {
        return RuleState.builder()
                .systemComponent(systemComponent)
                .ruleId(ruleId)
                .ruleStateComment(StringUtils.hasText(stateComment) ? stateComment : null)
                .state(state)
                .build();
    }
}
