package ch.admin.bit.jeap.governance.domain.rule;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RuleEvaluatorTest {

    private final RuleRepository ruleRepository = mock(RuleRepository.class);
    private final RuleEvaluator ruleEvaluator = new RuleEvaluator(ruleRepository);

    private final SystemComponent component = SystemComponent.builder()
            .name("my-service")
            .state(State.OK)
            .type(ComponentType.BACKEND_SERVICE)
            .build();

    @Test
    void activeRule_delegatesToRuleEvaluate() {
        Rule rule = okRule("enforce-oauth2", 10);
        var evaluation = new RuleEvaluation(rule, new RuleParameters(Map.of()), RuleActivationState.ACTIVE);
        when(ruleRepository.getRulesToEvaluateForComponent(component)).thenReturn(List.of(evaluation));

        List<RuleEvaluationResult> results = ruleEvaluator.evaluateRulesForComponent(component);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().state()).isEqualTo(State.OK);
        assertThat(results.getFirst().ruleId()).isEqualTo(RuleId.of("enforce-oauth2"));
    }

    @Test
    void exemptedRule_returnsDisabledState() {
        Rule rule = okRule("enforce-oauth2", 10);
        var evaluation = new RuleEvaluation(rule, new RuleParameters(Map.of()), RuleActivationState.EXEMPTED);
        when(ruleRepository.getRulesToEvaluateForComponent(component)).thenReturn(List.of(evaluation));

        List<RuleEvaluationResult> results = ruleEvaluator.evaluateRulesForComponent(component);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().state()).isEqualTo(State.DISABLED);
    }

    @Test
    void exemptedUntilRule_returnsPausedState() {
        Rule rule = okRule("enforce-oauth2", 10);
        var evaluation = new RuleEvaluation(rule, new RuleParameters(Map.of()), RuleActivationState.EXEMPTED_UNTIL);
        when(ruleRepository.getRulesToEvaluateForComponent(component)).thenReturn(List.of(evaluation));

        List<RuleEvaluationResult> results = ruleEvaluator.evaluateRulesForComponent(component);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().state()).isEqualTo(State.PAUSED);
    }

    @Test
    void activeFailingRule_returnsFailState() {
        Rule rule = failingRule("check-tls", 5);
        var evaluation = new RuleEvaluation(rule, new RuleParameters(Map.of()), RuleActivationState.ACTIVE);
        when(ruleRepository.getRulesToEvaluateForComponent(component)).thenReturn(List.of(evaluation));

        List<RuleEvaluationResult> results = ruleEvaluator.evaluateRulesForComponent(component);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().state()).isEqualTo(State.FAIL);
    }

    @Test
    void multipleRules_evaluatedIndependently() {
        Rule okRule = okRule("enforce-oauth2", 10);
        Rule failRule = failingRule("check-tls", 5);
        Rule exemptedRule = okRule("deprecated-rule", 3);

        var evaluations = List.of(
                new RuleEvaluation(okRule, new RuleParameters(Map.of()), RuleActivationState.ACTIVE),
                new RuleEvaluation(failRule, new RuleParameters(Map.of()), RuleActivationState.ACTIVE),
                new RuleEvaluation(exemptedRule, new RuleParameters(Map.of()), RuleActivationState.EXEMPTED)
        );
        when(ruleRepository.getRulesToEvaluateForComponent(component)).thenReturn(evaluations);

        List<RuleEvaluationResult> results = ruleEvaluator.evaluateRulesForComponent(component);

        assertThat(results).hasSize(3);
        assertThat(results.get(0).state()).isEqualTo(State.OK);
        assertThat(results.get(1).state()).isEqualTo(State.FAIL);
        assertThat(results.get(2).state()).isEqualTo(State.DISABLED);
    }

    @Test
    void noRules_returnsEmptyList() {
        when(ruleRepository.getRulesToEvaluateForComponent(component)).thenReturn(List.of());

        List<RuleEvaluationResult> results = ruleEvaluator.evaluateRulesForComponent(component);

        assertThat(results).isEmpty();
    }

    @Test
    void activeRule_receivesParametersFromEvaluation() {
        var parameters = new RuleParameters(Map.of("threshold", "10"));
        Rule rule = new Rule() {
            @Override
            public RuleMetadata metadata() {
                return new RuleMetadata(RuleId.of("param-rule"), "Param Rule", "http://docs", 1);
            }

            @Override
            public RuleEvaluationResult evaluate(SystemComponent systemComponent, RuleParameters ruleParameters) {
                // Rule uses parameters to decide outcome
                if ("10".equals(ruleParameters.parameters().get("threshold"))) {
                    return RuleEvaluationResult.ok(new RuleEvaluation(this, ruleParameters, RuleActivationState.ACTIVE));
                }
                return RuleEvaluationResult.failed(new RuleEvaluation(this, ruleParameters, RuleActivationState.ACTIVE));
            }
        };
        var evaluation = new RuleEvaluation(rule, parameters, RuleActivationState.ACTIVE);
        when(ruleRepository.getRulesToEvaluateForComponent(component)).thenReturn(List.of(evaluation));

        List<RuleEvaluationResult> results = ruleEvaluator.evaluateRulesForComponent(component);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().state()).isEqualTo(State.OK);
    }

    @Test
    void exemptedRule_doesNotCallRuleEvaluate() {
        // Even if the rule would fail when evaluated, exemption skips evaluation
        Rule failRule = failingRule("enforce-oauth2", 10);
        var evaluation = new RuleEvaluation(failRule, new RuleParameters(Map.of()), RuleActivationState.EXEMPTED);
        when(ruleRepository.getRulesToEvaluateForComponent(component)).thenReturn(List.of(evaluation));

        List<RuleEvaluationResult> results = ruleEvaluator.evaluateRulesForComponent(component);

        assertThat(results.getFirst().state()).isEqualTo(State.DISABLED);
    }

    private Rule okRule(String id, int weight) {
        return new Rule() {
            @Override
            public RuleMetadata metadata() {
                return new RuleMetadata(RuleId.of(id), "Rule " + id, "http://docs/" + id, weight);
            }

            @Override
            public RuleEvaluationResult evaluate(SystemComponent systemComponent, RuleParameters ruleParameters) {
                return RuleEvaluationResult.ok(new RuleEvaluation(this, ruleParameters, RuleActivationState.ACTIVE));
            }
        };
    }

    private Rule failingRule(String id, int weight) {
        return new Rule() {
            @Override
            public RuleMetadata metadata() {
                return new RuleMetadata(RuleId.of(id), "Rule " + id, "http://docs/" + id, weight);
            }

            @Override
            public RuleEvaluationResult evaluate(SystemComponent systemComponent, RuleParameters ruleParameters) {
                return RuleEvaluationResult.failed(new RuleEvaluation(this, ruleParameters, RuleActivationState.ACTIVE));
            }
        };
    }
}
