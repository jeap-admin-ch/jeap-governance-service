package ch.admin.bit.jeap.governance.domain.rule;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEvaluationResultTest {

    private static final RuleId RULE_ID = RuleId.of("test-rule");

    @Test
    void ok_returnsOkState() {
        var result = RuleEvaluationResult.ok(RULE_ID);

        assertThat(result.ruleId()).isEqualTo(RULE_ID);
        assertThat(result.state()).isEqualTo(State.OK);
        assertThat(result.stateComment()).isNull();
    }

    @Test
    void failed_returnsFailState() {
        var result = RuleEvaluationResult.failed(RULE_ID);

        assertThat(result.ruleId()).isEqualTo(RULE_ID);
        assertThat(result.state()).isEqualTo(State.FAIL);
        assertThat(result.stateComment()).isNull();
    }

    @Test
    void failed_withComment_returnsFailStateWithComment() {
        var result = RuleEvaluationResult.failed(RULE_ID, "missing OAuth2");

        assertThat(result.ruleId()).isEqualTo(RULE_ID);
        assertThat(result.state()).isEqualTo(State.FAIL);
        assertThat(result.stateComment()).isEqualTo("missing OAuth2");
    }

    @Test
    void exempted_returnsDisabledState() {
        var result = RuleEvaluationResult.exempted(RULE_ID);

        assertThat(result.ruleId()).isEqualTo(RULE_ID);
        assertThat(result.state()).isEqualTo(State.DISABLED);
        assertThat(result.stateComment()).isNull();
    }

    @Test
    void exemptedUntil_returnsPausedState() {
        var result = RuleEvaluationResult.exemptedUntil(RULE_ID);

        assertThat(result.ruleId()).isEqualTo(RULE_ID);
        assertThat(result.state()).isEqualTo(State.PAUSED);
        assertThat(result.stateComment()).isNull();
    }

    @Test
    void isOk_trueForOkState() {
        var result = RuleEvaluationResult.ok(RULE_ID);

        assertThat(result.ruleId()).isEqualTo(RULE_ID);
        assertThat(result.isOk()).isTrue();
    }

    @Test
    void isOk_falseForFailState() {
        var result = RuleEvaluationResult.failed(RULE_ID);

        assertThat(result.ruleId()).isEqualTo(RULE_ID);
        assertThat(result.isOk()).isFalse();
    }

    @Test
    void isOk_falseForDisabledState() {
        var result = RuleEvaluationResult.exempted(RULE_ID);

        assertThat(result.ruleId()).isEqualTo(RULE_ID);
        assertThat(result.isOk()).isFalse();
    }

    @Test
    void isOk_falseForPausedState() {
        var result = RuleEvaluationResult.exemptedUntil(RULE_ID);

        assertThat(result.ruleId()).isEqualTo(RULE_ID);
        assertThat(result.isOk()).isFalse();
    }

    @Test
    void ruleId_returnsRuleIdFromEvaluation() {
        var result = RuleEvaluationResult.ok(RULE_ID);

        assertThat(result.ruleId()).isEqualTo(RULE_ID);
    }

    @Test
    void toRuleState_createsRuleStateForComponent() {
        var component = SystemComponent.builder()
                .name("my-service")
                .type(ComponentType.BACKEND_SERVICE)
                .build();
        var result = RuleEvaluationResult.failed(RULE_ID, "non-compliant");

        var ruleState = result.toRuleState(component);

        assertThat(ruleState.getRuleId()).isEqualTo(RULE_ID.id());
        assertThat(ruleState.getSystemComponent()).isEqualTo(component);
        assertThat(ruleState.getState()).isEqualTo(State.FAIL);
        assertThat(ruleState.getStateComment()).isEqualTo("non-compliant");
    }

    @Test
    void toRuleState_blankComment_isStoredAsNull() {
        var component = SystemComponent.builder()
                .name("my-service")
                .type(ComponentType.BACKEND_SERVICE)
                .build();
        var result = RuleEvaluationResult.failed(RULE_ID, "   ");

        var ruleState = result.toRuleState(component);

        assertThat(ruleState.getStateComment()).isNull();
    }
}
