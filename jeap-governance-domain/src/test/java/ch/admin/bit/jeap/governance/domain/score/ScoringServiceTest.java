package ch.admin.bit.jeap.governance.domain.score;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.SystemRepository;
import ch.admin.bit.jeap.governance.domain.rule.RuleEvaluationResult;
import ch.admin.bit.jeap.governance.domain.rule.RuleEvaluationService;
import ch.admin.bit.jeap.governance.domain.rule.RuleId;
import ch.admin.bit.jeap.governance.domain.rule.RuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ScoringServiceTest {

    private static final Long SYSTEM_ID = 1L;

    private final RuleRepository ruleRepository = mock(RuleRepository.class);
    private final ComponentScoreCalculator componentScoreCalculator = new ComponentScoreCalculator(ruleRepository);
    private final ComponentScoreRepository componentScoreRepository = mock(ComponentScoreRepository.class);
    private final SystemScoreCalculator systemScoreCalculator = new SystemScoreCalculator();
    private final SystemScoreRepository systemScoreRepository = mock(SystemScoreRepository.class);
    private final RuleEvaluationService ruleEvaluationService = mock(RuleEvaluationService.class);
    private final SystemRepository systemRepository = mock(SystemRepository.class);

    private final ScoringService scoringService = new ScoringService(
            componentScoreCalculator, componentScoreRepository,
            systemScoreCalculator, systemScoreRepository,
            ruleEvaluationService, systemRepository);

    private static final LocalDate DAY = LocalDate.of(2025, Month.JUNE, 15);

    @BeforeEach
    void setUp() {
        when(ruleRepository.getActiveRuleWeights()).thenReturn(Map.of(
                RuleId.of("rule-1"), 10,
                RuleId.of("rule-2"), 5
        ));
    }

    @Test
    void updateSystemScore_evaluatesRulesForEachComponent() {
        System system = systemWithComponents("service-a", "service-b");
        when(systemRepository.findById(SYSTEM_ID)).thenReturn(Optional.of(system));
        when(ruleEvaluationService.updateRuleStatesForComponent(any())).thenReturn(List.of());

        scoringService.updateSystemScore(SYSTEM_ID, DAY);

        verify(ruleEvaluationService, times(2)).updateRuleStatesForComponent(any());
    }

    @Test
    void updateSystemScore_savesComponentScores() {
        System system = systemWithComponents("service-a");
        when(systemRepository.findById(SYSTEM_ID)).thenReturn(Optional.of(system));
        when(ruleEvaluationService.updateRuleStatesForComponent(any())).thenReturn(List.of());

        scoringService.updateSystemScore(SYSTEM_ID, DAY);

        verify(componentScoreRepository).saveOrReplaceAllForSystemAndDay(eq(system), any(), eq(DAY));
    }

    @Test
    void updateSystemScore_savesSystemScore() {
        System system = systemWithComponents("service-a");
        when(systemRepository.findById(SYSTEM_ID)).thenReturn(Optional.of(system));
        when(ruleEvaluationService.updateRuleStatesForComponent(any())).thenReturn(List.of());

        scoringService.updateSystemScore(SYSTEM_ID, DAY);

        verify(systemScoreRepository).save(argThat(score -> {
            assertThat(score.getSystem()).isEqualTo(system);
            assertThat(score.getScore()).isEqualTo(100);
            return true;
        }));
    }

    @Test
    void updateSystemScore_noComponents_savesScoreOf100() {
        System system = systemWithComponents();
        when(systemRepository.findById(SYSTEM_ID)).thenReturn(Optional.of(system));
        when(ruleEvaluationService.updateRuleStatesForComponent(any())).thenReturn(List.of());

        scoringService.updateSystemScore(SYSTEM_ID, DAY);

        verify(componentScoreRepository).saveOrReplaceAllForSystemAndDay(system, List.of(), DAY);
        verify(systemScoreRepository).save(argThat(score -> score.getScore() == 100));
    }

    @Test
    void updateSystemScore_usesRuleEvaluationResultsForScoring() {
        System system = systemWithComponents("service-a");
        var component = system.getSystemComponents().getFirst();
        when(systemRepository.findById(SYSTEM_ID)).thenReturn(Optional.of(system));

        List<RuleEvaluationResult> results = List.of(
                okResult("rule-1"),
                okResult("rule-2")
        );
        when(ruleEvaluationService.updateRuleStatesForComponent(component)).thenReturn(results);

        scoringService.updateSystemScore(SYSTEM_ID, DAY);

        // All rules OK → component score 100 → system score 100
        verify(systemScoreRepository).save(argThat(score -> score.getScore() == 100));
    }

    @Test
    void updateSystemScore_returnsAllRuleEvaluationResults() {
        System system = systemWithComponents("service-a");
        var component = system.getSystemComponents().getFirst();
        when(systemRepository.findById(SYSTEM_ID)).thenReturn(Optional.of(system));

        List<RuleEvaluationResult> results = List.of(
                okResult("rule-1"),
                okResult("rule-2")
        );
        when(ruleEvaluationService.updateRuleStatesForComponent(component)).thenReturn(results);

        var allResults = scoringService.updateSystemScore(SYSTEM_ID, DAY);

        assertThat(allResults)
                .hasSize(2)
                .containsExactlyElementsOf(results);
    }

    @Test
    void updateSystemScore_ignoresGatewayComponents() {
        List<SystemComponent> components = List.of(
                SystemComponent.builder().name("service-a").type(ComponentType.BACKEND_SERVICE).build(),
                SystemComponent.builder().name("GATEWAY-api-gw").type(ComponentType.GATEWAY).build()
        );
        System system = System.builder().name("test-system").aliases(Set.of()).systemComponents(components).build();
        when(systemRepository.findById(SYSTEM_ID)).thenReturn(Optional.of(system));
        when(ruleEvaluationService.updateRuleStatesForComponent(any())).thenReturn(List.of());

        scoringService.updateSystemScore(SYSTEM_ID, DAY);

        // Only the BACKEND_SERVICE component should be evaluated, not the GATEWAY one
        verify(ruleEvaluationService, times(1)).updateRuleStatesForComponent(any());
        verify(componentScoreRepository).saveOrReplaceAllForSystemAndDay(eq(system),
                argThat(scores -> scores.size() == 1), eq(DAY));
    }

    private System systemWithComponents(String... componentNames) {
        List<SystemComponent> components = java.util.Arrays.stream(componentNames)
                .map(n -> SystemComponent.builder()
                        .name(n)
                        .type(ComponentType.BACKEND_SERVICE)
                        .build())
                .toList();
        return System.builder()
                .name("test-system")
                .aliases(Set.of())
                .systemComponents(components)
                .build();
    }

    private RuleEvaluationResult okResult(String ruleId) {
        return RuleEvaluationResult.ok(RuleId.of(ruleId));
    }
}
