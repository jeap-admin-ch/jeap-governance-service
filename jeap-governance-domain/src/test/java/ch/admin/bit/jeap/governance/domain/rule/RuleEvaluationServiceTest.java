package ch.admin.bit.jeap.governance.domain.rule;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.plugin.rule.Rule;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleMetadata;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleParameters;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RuleEvaluationServiceTest {

    private final RuleRepository ruleRepository = mock(RuleRepository.class);
    private final RuleStateRepository ruleStateRepository = mock(RuleStateRepository.class);
    private final RuleEvaluator ruleEvaluator = new RuleEvaluator(ruleRepository);
    private final RuleEvaluationService service = new RuleEvaluationService(ruleEvaluator, ruleStateRepository);

    private final SystemComponent component = SystemComponent.builder()
            .name("my-service")
            .type(ComponentType.BACKEND_SERVICE)
            .build();

    @Test
    void updateRuleStatesForComponent_returnsEvaluationResults() {
        Rule rule = okRule("enforce-oauth2");
        var evaluation = new RuleEvaluation(rule, new RuleParameters(Map.of()), RuleActivationState.ACTIVE);
        when(ruleRepository.getRulesToEvaluateForComponent(component)).thenReturn(List.of(evaluation));
        when(ruleStateRepository.findBySystemComponentAndRuleId(eq(component), any()))
                .thenReturn(Optional.of(existingRuleState(rule.metadata().ruleId())));

        List<RuleEvaluationResult> results = service.updateRuleStatesForComponent(component);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().state()).isEqualTo(State.OK);
    }

    @Test
    void updateRuleStatesForComponent_savesAllRuleStates() {
        Rule rule1 = okRule("rule-1");
        Rule rule2 = failingRule("rule-2");
        var evaluations = List.of(
                new RuleEvaluation(rule1, new RuleParameters(Map.of()), RuleActivationState.ACTIVE),
                new RuleEvaluation(rule2, new RuleParameters(Map.of()), RuleActivationState.ACTIVE)
        );
        when(ruleRepository.getRulesToEvaluateForComponent(component)).thenReturn(evaluations);
        when(ruleStateRepository.findBySystemComponentAndRuleId(eq(component), any())).thenReturn(Optional.empty());

        service.updateRuleStatesForComponent(component);

        verify(ruleStateRepository).saveAll(argThat(list -> list.size() == 2));
    }

    @Test
    void updateRuleStatesForComponent_existingState_reusesAndModifiesExistingRuleState() {
        Rule rule = failingRule("enforce-oauth2");
        var evaluation = new RuleEvaluation(rule, new RuleParameters(Map.of()), RuleActivationState.ACTIVE);
        when(ruleRepository.getRulesToEvaluateForComponent(component)).thenReturn(List.of(evaluation));
        var existingState = existingRuleState(rule.metadata().ruleId());
        when(ruleStateRepository.findBySystemComponentAndRuleId(component, rule.metadata().ruleId()))
                .thenReturn(Optional.of(existingState));

        service.updateRuleStatesForComponent(component);

        verify(ruleStateRepository).saveAll(argThat(list -> list.getFirst() == existingState));
        assertThat(existingState.getState()).isEqualTo(State.FAIL);
    }

    @Test
    void updateRuleStatesForComponent_noRules_savesEmptyList() {
        when(ruleRepository.getRulesToEvaluateForComponent(component)).thenReturn(List.of());

        List<RuleEvaluationResult> results = service.updateRuleStatesForComponent(component);

        assertThat(results).isEmpty();
        verify(ruleStateRepository).saveAll(List.of());
    }

    @Test
    void updateRuleStatesForComponent_newState_createsNewRuleState() {
        Rule rule = okRule("new-rule");
        var evaluation = new RuleEvaluation(rule, new RuleParameters(Map.of()), RuleActivationState.ACTIVE);
        when(ruleRepository.getRulesToEvaluateForComponent(component)).thenReturn(List.of(evaluation));
        when(ruleStateRepository.findBySystemComponentAndRuleId(component, rule.metadata().ruleId()))
                .thenReturn(Optional.empty());

        service.updateRuleStatesForComponent(component);

        verify(ruleStateRepository).saveAll(argThat(list -> {
            RuleState saved = list.getFirst();
            return saved.getRuleId().equals("new-rule") &&
                    saved.getState() == State.OK &&
                    saved.getSystemComponent() == component;
        }));
    }

    private RuleState existingRuleState(RuleId ruleId) {
        return RuleState.builder()
                .ruleId(ruleId)
                .systemComponent(component)
                .state(State.OK)
                .build();
    }

    private Rule okRule(String id) {
        return new Rule() {
            @Override
            public RuleMetadata metadata() {
                return new RuleMetadata(RuleId.of(id), "Rule " + id);
            }

            @Override
            public RuleResult evaluate(SystemComponent systemComponent, RuleParameters ruleParameters) {
                return RuleResult.ok();
            }
        };
    }

    private Rule failingRule(String id) {
        return new Rule() {
            @Override
            public RuleMetadata metadata() {
                return new RuleMetadata(RuleId.of(id), "Rule " + id);
            }

            @Override
            public RuleResult evaluate(SystemComponent systemComponent, RuleParameters ruleParameters) {
                return RuleResult.failed();
            }
        };
    }
}
