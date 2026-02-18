package ch.admin.bit.jeap.governance.domain.score;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.plugin.rule.Rule;
import ch.admin.bit.jeap.governance.domain.rule.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ScoringServiceTest {

    private final ComponentScoreCalculator componentScoreCalculator = new ComponentScoreCalculator();
    private final ComponentScoreRepository componentScoreRepository = mock(ComponentScoreRepository.class);
    private final SystemScoreCalculator systemScoreCalculator = new SystemScoreCalculator();
    private final SystemScoreRepository systemScoreRepository = mock(SystemScoreRepository.class);
    private final RuleEvaluationService ruleEvaluationService = mock(RuleEvaluationService.class);

    private final ScoringService scoringService = new ScoringService(
            componentScoreCalculator, componentScoreRepository,
            systemScoreCalculator, systemScoreRepository,
            ruleEvaluationService);

    private static final LocalDate DAY = LocalDate.of(2025, 6, 15);

    @Test
    void updateSystemScore_evaluatesRulesForEachComponent() {
        System system = systemWithComponents("service-a", "service-b");
        when(ruleEvaluationService.updateRuleStatesForComponent(any())).thenReturn(List.of());

        scoringService.updateSystemScore(system, DAY);

        verify(ruleEvaluationService, times(2)).updateRuleStatesForComponent(any());
    }

    @Test
    void updateSystemScore_savesComponentScores() {
        System system = systemWithComponents("service-a");
        when(ruleEvaluationService.updateRuleStatesForComponent(any())).thenReturn(List.of());

        scoringService.updateSystemScore(system, DAY);

        verify(componentScoreRepository).saveOrReplaceAllForSystemAndDay(eq(system), any(), eq(DAY));
    }

    @Test
    void updateSystemScore_savesSystemScore() {
        System system = systemWithComponents("service-a");
        when(ruleEvaluationService.updateRuleStatesForComponent(any())).thenReturn(List.of());

        scoringService.updateSystemScore(system, DAY);

        verify(systemScoreRepository).save(argThat(score -> {
            assertThat(score.getSystem()).isEqualTo(system);
            assertThat(score.getScore()).isEqualTo(100);
            return true;
        }));
    }

    @Test
    void updateSystemScore_noComponents_savesScoreOf100() {
        System system = systemWithComponents();
        when(ruleEvaluationService.updateRuleStatesForComponent(any())).thenReturn(List.of());

        scoringService.updateSystemScore(system, DAY);

        verify(componentScoreRepository).saveOrReplaceAllForSystemAndDay(eq(system), eq(List.of()), eq(DAY));
        verify(systemScoreRepository).save(argThat(score -> score.getScore() == 100));
    }

    @Test
    void updateSystemScore_usesRuleEvaluationResultsForScoring() {
        System system = systemWithComponents("service-a");
        var component = system.getSystemComponents().getFirst();

        List<RuleEvaluationResult> results = List.of(
                okResult("rule-1", 10),
                okResult("rule-2", 5)
        );
        when(ruleEvaluationService.updateRuleStatesForComponent(component)).thenReturn(results);

        scoringService.updateSystemScore(system, DAY);

        // All rules OK → component score 100 → system score 100
        verify(systemScoreRepository).save(argThat(score -> score.getScore() == 100));
    }

    @Test
    void updateSystemScore_returnsAllRuleEvaluationResults() {
        System system = systemWithComponents("service-a");
        var component = system.getSystemComponents().getFirst();

        List<RuleEvaluationResult> results = List.of(
                okResult("rule-1", 10),
                okResult("rule-2", 5)
        );
        when(ruleEvaluationService.updateRuleStatesForComponent(component)).thenReturn(results);

        var allResults = scoringService.updateSystemScore(system, DAY);

        assertThat(allResults).hasSize(2);
        assertThat(allResults).containsExactlyElementsOf(results);
    }

    private System systemWithComponents(String... componentNames) {
        List<SystemComponent> components = java.util.Arrays.stream(componentNames)
                .map(n -> SystemComponent.builder()
                        .name(n)
                        .state(State.OK)
                        .type(ComponentType.BACKEND_SERVICE)
                        .build())
                .toList();
        return System.builder()
                .name("test-system")
                .aliases(Set.of())
                .systemComponents(components)
                .state(State.OK)
                .build();
    }

    private RuleEvaluationResult okResult(String ruleId, int weight) {
        Rule rule = new Rule() {
            @Override
            public RuleMetadata metadata() {
                return new RuleMetadata(RuleId.of(ruleId), "Rule", "http://docs", weight);
            }

            @Override
            public RuleEvaluationResult evaluate(SystemComponent systemComponent, RuleParameters ruleParameters) {
                throw new UnsupportedOperationException();
            }
        };
        var evaluation = new RuleEvaluation(rule, new RuleParameters(Map.of()), RuleActivationState.ACTIVE);
        return new RuleEvaluationResult(evaluation, State.OK, null);
    }
}
