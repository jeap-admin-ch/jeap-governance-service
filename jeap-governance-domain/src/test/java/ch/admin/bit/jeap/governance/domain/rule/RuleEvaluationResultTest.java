package ch.admin.bit.jeap.governance.domain.rule;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.plugin.rule.Rule;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEvaluationResultTest {

    private static final RuleId RULE_ID = RuleId.of("test-rule");
    private static final int WEIGHT = 5;

    private final Rule testRule = new Rule() {
        @Override
        public RuleMetadata metadata() {
            return new RuleMetadata(RULE_ID, "Test Rule", "http://docs", WEIGHT);
        }

        @Override
        public RuleEvaluationResult evaluate(SystemComponent systemComponent, RuleParameters ruleParameters) {
            return RuleEvaluationResult.ok(new RuleEvaluation(this, ruleParameters, RuleActivationState.ACTIVE));
        }
    };

    private final RuleEvaluation activeEvaluation = new RuleEvaluation(testRule, new RuleParameters(Map.of()), RuleActivationState.ACTIVE);

    @Test
    void ok_returnsOkState() {
        var result = RuleEvaluationResult.ok(activeEvaluation);

        assertThat(result.state()).isEqualTo(State.OK);
        assertThat(result.stateComment()).isNull();
    }

    @Test
    void failed_returnsFailState() {
        var result = RuleEvaluationResult.failed(activeEvaluation);

        assertThat(result.state()).isEqualTo(State.FAIL);
        assertThat(result.stateComment()).isNull();
    }

    @Test
    void failed_withComment_returnsFailStateWithComment() {
        var result = RuleEvaluationResult.failed(activeEvaluation, "missing OAuth2");

        assertThat(result.state()).isEqualTo(State.FAIL);
        assertThat(result.stateComment()).isEqualTo("missing OAuth2");
    }

    @Test
    void exempted_returnsDisabledState() {
        var result = RuleEvaluationResult.exempted(activeEvaluation);

        assertThat(result.state()).isEqualTo(State.DISABLED);
        assertThat(result.stateComment()).isNull();
    }

    @Test
    void exemptedUntil_returnsPausedState() {
        var result = RuleEvaluationResult.exemptedUntil(activeEvaluation);

        assertThat(result.state()).isEqualTo(State.PAUSED);
        assertThat(result.stateComment()).isNull();
    }

    @Test
    void isOk_trueForOkState() {
        var result = RuleEvaluationResult.ok(activeEvaluation);

        assertThat(result.isOk()).isTrue();
    }

    @Test
    void isOk_falseForFailState() {
        var result = RuleEvaluationResult.failed(activeEvaluation);

        assertThat(result.isOk()).isFalse();
    }

    @Test
    void isOk_falseForDisabledState() {
        var result = RuleEvaluationResult.exempted(activeEvaluation);

        assertThat(result.isOk()).isFalse();
    }

    @Test
    void isOk_falseForPausedState() {
        var result = RuleEvaluationResult.exemptedUntil(activeEvaluation);

        assertThat(result.isOk()).isFalse();
    }

    @Test
    void ruleWeight_returnsWeightFromMetadata() {
        var result = RuleEvaluationResult.ok(activeEvaluation);

        assertThat(result.ruleWeight()).isEqualTo(WEIGHT);
    }

    @Test
    void ruleId_returnsRuleIdFromEvaluation() {
        var result = RuleEvaluationResult.ok(activeEvaluation);

        assertThat(result.ruleId()).isEqualTo(RULE_ID);
    }

    @Test
    void toRuleState_createsRuleStateForComponent() {
        var component = SystemComponent.builder()
                .name("my-service")
                .state(State.OK)
                .type(ComponentType.BACKEND_SERVICE)
                .build();
        var result = RuleEvaluationResult.failed(activeEvaluation, "non-compliant");

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
                .state(State.OK)
                .type(ComponentType.BACKEND_SERVICE)
                .build();
        var result = RuleEvaluationResult.failed(activeEvaluation, "   ");

        var ruleState = result.toRuleState(component);

        assertThat(ruleState.getStateComment()).isNull();
    }
}
