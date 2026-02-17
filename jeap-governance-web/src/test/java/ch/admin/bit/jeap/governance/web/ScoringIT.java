package ch.admin.bit.jeap.governance.web;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.SystemRepository;
import ch.admin.bit.jeap.governance.domain.rule.*;
import ch.admin.bit.jeap.governance.domain.score.ComponentScoreRepository;
import ch.admin.bit.jeap.governance.domain.score.ScoringService;
import ch.admin.bit.jeap.governance.domain.score.SystemScoreRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("scoring-it")
@Import(ScoringIT.TestRuleConfig.class)
class ScoringIT extends GovernanceIntegrationTestBase {

    static final AtomicReference<State> FLIPPING_RULE_STATE = new AtomicReference<>(State.FAIL);

    static class TestRuleConfig {

        @Bean
        Rule flippingRule() {
            return new Rule() {
                @Override
                public RuleMetadata metadata() {
                    return new RuleMetadata(RuleId.of("flipping-rule"), "Flipping Rule", null, 10);
                }

                @Override
                public RuleEvaluationResult evaluate(SystemComponent systemComponent, RuleParameters ruleParameters) {
                    var state = FLIPPING_RULE_STATE.get();
                    var comment = state == State.FAIL ? "not yet compliant" : null;
                    return new RuleEvaluationResult(
                            new RuleEvaluation(this, ruleParameters, RuleActivationState.ACTIVE), state, comment);
                }
            };
        }

        @Bean
        Rule alwaysOkRule() {
            return new Rule() {
                @Override
                public RuleMetadata metadata() {
                    return new RuleMetadata(RuleId.of("always-ok-rule"), "Always OK", null, 10);
                }

                @Override
                public RuleEvaluationResult evaluate(SystemComponent systemComponent, RuleParameters ruleParameters) {
                    return new RuleEvaluationResult(
                            new RuleEvaluation(this, ruleParameters, RuleActivationState.ACTIVE), State.OK, null);
                }
            };
        }

        @Bean
        Rule alwaysFailRule() {
            return new Rule() {
                @Override
                public RuleMetadata metadata() {
                    return new RuleMetadata(RuleId.of("always-fail-rule"), "Always Fail", null, 10);
                }

                @Override
                public RuleEvaluationResult evaluate(SystemComponent systemComponent, RuleParameters ruleParameters) {
                    return new RuleEvaluationResult(
                            new RuleEvaluation(this, ruleParameters, RuleActivationState.ACTIVE), State.FAIL, "non-compliant");
                }
            };
        }

        @Bean
        Rule exemptedRule() {
            return new Rule() {
                @Override
                public RuleMetadata metadata() {
                    return new RuleMetadata(RuleId.of("exempted-rule"), "Exempted Rule", null, 10);
                }

                @Override
                public RuleEvaluationResult evaluate(SystemComponent systemComponent, RuleParameters ruleParameters) {
                    return new RuleEvaluationResult(
                            new RuleEvaluation(this, ruleParameters, RuleActivationState.ACTIVE), State.OK, null);
                }
            };
        }
    }

    @Autowired
    private SystemRepository systemRepository;

    @Autowired
    private ScoringService scoringService;

    @Autowired
    private RuleStateRepository ruleStateRepository;

    @Autowired
    private ComponentScoreRepository componentScoreRepository;

    @Autowired
    private SystemScoreRepository systemScoreRepository;

    @Test
    void scoring_evaluatesRulesAndPersistsScores() {
        FLIPPING_RULE_STATE.set(State.FAIL);

        // Given: a system with two components
        var system = systemRepository.add(System.builder()
                .name("test-system")
                .state(State.OK)
                .aliases(Set.of())
                .systemComponents(List.of(
                        SystemComponent.builder().name("service-a").state(State.OK).type(ComponentType.BACKEND_SERVICE).build(),
                        SystemComponent.builder().name("service-b").state(State.OK).type(ComponentType.BACKEND_SERVICE).build()
                ))
                .build());

        // When: scoring is triggered
        scoringService.updateSystemScore(system);

        // Then: rule states are persisted for both components
        var serviceA = findComponent(system, "service-a");
        var serviceB = findComponent(system, "service-b");

        // service-a: all rules active, no exemptions
        assertRuleState(serviceA, "always-ok-rule", State.OK, null);
        assertRuleState(serviceA, "always-fail-rule", State.FAIL, "non-compliant");
        assertRuleState(serviceA, "flipping-rule", State.FAIL, "not yet compliant");
        // service-a has no exemption, so exempted-rule is evaluated as ACTIVE (OK)
        assertRuleState(serviceA, "exempted-rule", State.OK, null);

        // service-b: exempted-rule is exempted -> DISABLED state
        assertRuleState(serviceB, "always-ok-rule", State.OK, null);
        assertRuleState(serviceB, "always-fail-rule", State.FAIL, "non-compliant");
        assertRuleState(serviceB, "flipping-rule", State.FAIL, "not yet compliant");
        assertRuleState(serviceB, "exempted-rule", State.DISABLED, null);

        // Then: component scores are persisted
        var today = LocalDate.now();
        var scoreA = componentScoreRepository.findBySystemComponentAndDay(serviceA, today);
        assertThat(scoreA).isPresent();
        // service-a: 4 active rules, weight 10 each. OK=20 (always-ok + exempted-rule), total=40 -> 50%
        assertThat(scoreA.get().getScore()).isEqualTo(50);

        var scoreB = componentScoreRepository.findBySystemComponentAndDay(serviceB, today);
        assertThat(scoreB).isPresent();
        // service-b: 3 active rules (DISABLED excluded), OK=10 (always-ok), total=30 -> 33%
        assertThat(scoreB.get().getScore()).isEqualTo(33);

        // Then: system score is the average of component scores
        var systemScore = systemScoreRepository.findBySystemAndDay(system, today);
        assertThat(systemScore).isPresent();
        // (50 + 33) / 2 = 41
        assertThat(systemScore.get().getScore()).isEqualTo(41);
    }

    @Test
    void scoring_updatesExistingRuleStates() {
        FLIPPING_RULE_STATE.set(State.FAIL);

        // Given: a system with one component
        var system = systemRepository.add(System.builder()
                .name("update-test-system")
                .state(State.OK)
                .aliases(Set.of())
                .systemComponents(List.of(
                        SystemComponent.builder().name("update-service").state(State.OK).type(ComponentType.BACKEND_SERVICE).build()
                ))
                .build());
        var component = findComponent(system, "update-service");

        // When: first scoring run
        scoringService.updateSystemScore(system);

        // Then: flipping-rule is FAIL
        assertRuleState(component, "flipping-rule", State.FAIL, "not yet compliant");
        var ruleStateAfterFirstRun = ruleStateRepository.findBySystemComponentAndRuleId(component, RuleId.of("flipping-rule"));
        var idAfterFirstRun = ruleStateAfterFirstRun.get().getId();

        // When: rule outcome changes and scoring runs again
        FLIPPING_RULE_STATE.set(State.OK);
        scoringService.updateSystemScore(system);

        // Then: existing rule state is updated (same ID, new state)
        var ruleStateAfterSecondRun = ruleStateRepository.findBySystemComponentAndRuleId(component, RuleId.of("flipping-rule"));
        assertThat(ruleStateAfterSecondRun).isPresent();
        assertThat(ruleStateAfterSecondRun.get().getId()).isEqualTo(idAfterFirstRun);
        assertThat(ruleStateAfterSecondRun.get().getState()).isEqualTo(State.OK);
        assertThat(ruleStateAfterSecondRun.get().getStateComment()).isNull();

        // Then: scores reflect the updated rule state
        var today = LocalDate.now();
        var score = componentScoreRepository.findBySystemComponentAndDay(component, today);
        assertThat(score).isPresent();
        // 4 active rules, OK=30 (always-ok + exempted-rule + flipping-rule), total=40 -> 75%
        assertThat(score.get().getScore()).isEqualTo(75);
    }

    private void assertRuleState(SystemComponent component, String ruleId, State expectedState, String expectedComment) {
        var ruleState = ruleStateRepository.findBySystemComponentAndRuleId(component, RuleId.of(ruleId));
        assertThat(ruleState).as("Rule state for %s on %s", ruleId, component.getName()).isPresent();
        assertThat(ruleState.get().getState()).isEqualTo(expectedState);
        assertThat(ruleState.get().getStateComment()).isEqualTo(expectedComment);
    }

    private static SystemComponent findComponent(System system, String name) {
        return system.getSystemComponents().stream()
                .filter(c -> c.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }
}
