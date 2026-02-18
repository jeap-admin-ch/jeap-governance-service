package ch.admin.bit.jeap.governance.domain.rule;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import org.springframework.util.StringUtils;

/**
 * The outcome of evaluating a rule against a component, carrying the resulting state and an optional comment for failed rules.
 */
public record RuleEvaluationResult(RuleEvaluation ruleEvaluation, State state, String stateComment) {

    static RuleEvaluationResult ok(RuleEvaluation ruleEvaluation) {
        return new RuleEvaluationResult(ruleEvaluation, State.OK, null);
    }

    static RuleEvaluationResult failed(RuleEvaluation ruleEvaluation) {
        return new RuleEvaluationResult(ruleEvaluation, State.FAIL, null);
    }

    static RuleEvaluationResult failed(RuleEvaluation ruleEvaluation, String stateComment) {
        return new RuleEvaluationResult(ruleEvaluation, State.FAIL, stateComment);
    }

    static RuleEvaluationResult exempted(RuleEvaluation ruleEvaluation) {
        return new RuleEvaluationResult(ruleEvaluation, State.DISABLED, null);
    }

    static RuleEvaluationResult exemptedUntil(RuleEvaluation ruleEvaluation) {
        return new RuleEvaluationResult(ruleEvaluation, State.PAUSED, null);
    }

    public int ruleWeight() {
        return ruleEvaluation.rule().metadata().weight();
    }

    public boolean isOk() {
        return state == State.OK;
    }

    RuleState toRuleState(SystemComponent systemComponent) {
        return RuleState.builder()
                .systemComponent(systemComponent)
                .ruleId(ruleEvaluation.rule().metadata().ruleId())
                .ruleStateComment(StringUtils.hasText(stateComment) ? stateComment : null)
                .state(state)
                .build();
    }

    RuleId ruleId() {
        return ruleEvaluation.rule().metadata().ruleId();
    }
}
