package ch.admin.bit.jeap.governance.web;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.SystemRepository;
import ch.admin.bit.jeap.governance.domain.plugin.rule.Rule;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleMetadata;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleParameters;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleResult;
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
import java.util.Map;
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
                    return new RuleMetadata(RuleId.of("flipping-rule"), "Flipping Rule");
                }

                @Override
                public RuleResult evaluate(SystemComponent systemComponent, RuleParameters ruleParameters) {
                    var state = FLIPPING_RULE_STATE.get();
                    var comment = state == State.FAIL ? "not yet compliant" : null;
                    return new RuleResult(state, comment);
                }
            };
        }

        @Bean
        Rule alwaysOkRule() {
            return new Rule() {
                @Override
                public RuleMetadata metadata() {
                    return new RuleMetadata(RuleId.of("always-ok-rule"), "Always OK");
                }

                @Override
                public RuleResult evaluate(SystemComponent systemComponent, RuleParameters ruleParameters) {
                    return RuleResult.ok();
                }
            };
        }

        @Bean
        Rule alwaysFailRule() {
            return new Rule() {
                @Override
                public RuleMetadata metadata() {
                    return new RuleMetadata(RuleId.of("always-fail-rule"), "Always Fail");
                }

                @Override
                public RuleResult evaluate(SystemComponent systemComponent, RuleParameters ruleParameters) {
                    return RuleResult.failed("non-compliant");
                }
            };
        }

        @Bean
        Rule exemptedRule() {
            return new Rule() {
                @Override
                public RuleMetadata metadata() {
                    return new RuleMetadata(RuleId.of("exempted-rule"), "Exempted Rule");
                }

                @Override
                public RuleResult evaluate(SystemComponent systemComponent, RuleParameters ruleParameters) {
                    return RuleResult.ok();
                }
            };
        }
    }

    @Autowired
    private SystemRepository systemRepository;

    @Autowired
    private ScoringService scoringService;

    @Autowired
    private RuleConformanceRateService ruleConformanceRateService;

    @Autowired
    private RuleStateRepository ruleStateRepository;

    @Autowired
    private RuleConformanceRateRepository ruleConformanceRateRepository;

    @Autowired
    private SystemRuleConformanceRateRepository systemRuleConformanceRateRepository;

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
                .aliases(Set.of())
                .systemComponents(List.of(
                        SystemComponent.builder().name("service-a").type(ComponentType.BACKEND_SERVICE).build(),
                        SystemComponent.builder().name("service-b").type(ComponentType.BACKEND_SERVICE).build()
                ))
                .build());

        var today = LocalDate.now();

        // When: scoring is triggered
        var results = scoringService.updateSystemScore(system.getId(), today);

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
        var scoreA = componentScoreRepository.findBySystemComponentAndDay(serviceA, today);
        assertThat(scoreA).isPresent();
        // service-a: 4 active rules. OK=30+10 (always-ok + exempted-rule), total=30+20+10+40=100 -> 40%
        assertThat(scoreA.get().getScore()).isEqualTo(40);

        var scoreB = componentScoreRepository.findBySystemComponentAndDay(serviceB, today);
        assertThat(scoreB).isPresent();
        // service-b: 3 active rules (DISABLED excluded). OK=30 (always-ok), total=30+20+40=90 -> 33%
        assertThat(scoreB.get().getScore()).isEqualTo(33);

        // Then: system score is the average of component scores
        var systemScore = systemScoreRepository.findBySystemAndDay(system, today);
        assertThat(systemScore).isPresent();
        // (40 + 33) / 2 = 36
        assertThat(systemScore.get().getScore()).isEqualTo(36);

        // Then: conformance rates are calculated and persisted
        ruleConformanceRateService.updateConformanceRates(results, today);

        // always-ok-rule: OK for both components -> 100%
        assertConformanceRate("always-ok-rule", today, 100);
        // always-fail-rule: FAIL for both components -> 0%
        assertConformanceRate("always-fail-rule", today, 0);
        // flipping-rule: FAIL for both components -> 0%
        assertConformanceRate("flipping-rule", today, 0);
        // exempted-rule: OK for service-a, DISABLED for service-b (excluded) -> 1 OK / 1 total = 100%
        assertConformanceRate("exempted-rule", today, 100);

        // Then: system conformance rates are calculated and persisted
        ruleConformanceRateService.updateSystemConformanceRates(Map.of(system.getId(), results), today);

        // always-ok-rule: OK for both -> 2 conformant / 2 total = 100%
        assertSystemConformanceRate(system.getId(), "always-ok-rule", today, 100);
        // always-fail-rule: FAIL for both -> 0 conformant / 2 total = 0%
        assertSystemConformanceRate(system.getId(), "always-fail-rule", today, 0);
        // flipping-rule: FAIL for both -> 0 conformant / 2 total = 0%
        assertSystemConformanceRate(system.getId(), "flipping-rule", today, 0);
        // exempted-rule: OK for service-a, DISABLED for service-b -> 2 conformant (OK+DISABLED) / 2 total = 100%
        assertSystemConformanceRate(system.getId(), "exempted-rule", today, 100);
    }

    @Test
    void scoring_updatesExistingRuleStates() {
        FLIPPING_RULE_STATE.set(State.FAIL);

        // Given: a system with one component
        var system = systemRepository.add(System.builder()
                .name("update-test-system")
                .aliases(Set.of())
                .systemComponents(List.of(
                        SystemComponent.builder().name("update-service").type(ComponentType.BACKEND_SERVICE).build()
                ))
                .build());
        var component = findComponent(system, "update-service");
        var today = LocalDate.now();

        // When: first scoring run
        var firstResults = scoringService.updateSystemScore(system.getId(), today);

        // Then: flipping-rule is FAIL
        assertRuleState(component, "flipping-rule", State.FAIL, "not yet compliant");
        var ruleStateAfterFirstRun = ruleStateRepository.findBySystemComponentAndRuleId(component, RuleId.of("flipping-rule"));
        var idAfterFirstRun = ruleStateAfterFirstRun.orElseThrow().getId();

        // Then: conformance rates after first run
        ruleConformanceRateService.updateConformanceRates(firstResults, today);
        // flipping-rule: FAIL for 1 component -> 0%
        assertConformanceRate("flipping-rule", today, 0);

        // Then: system conformance rates after first run
        ruleConformanceRateService.updateSystemConformanceRates(Map.of(system.getId(), firstResults), today);
        // flipping-rule: FAIL for 1 component -> 0 conformant / 1 total = 0%
        assertSystemConformanceRate(system.getId(), "flipping-rule", today, 0);

        // When: rule outcome changes and scoring runs again
        FLIPPING_RULE_STATE.set(State.OK);
        var secondResults = scoringService.updateSystemScore(system.getId(), today);

        // Then: existing rule state is updated (same ID, new state)
        var ruleStateAfterSecondRun = ruleStateRepository.findBySystemComponentAndRuleId(component, RuleId.of("flipping-rule"));
        assertThat(ruleStateAfterSecondRun).isPresent();
        assertThat(ruleStateAfterSecondRun.get().getId()).isEqualTo(idAfterFirstRun);
        assertThat(ruleStateAfterSecondRun.get().getState()).isEqualTo(State.OK);
        assertThat(ruleStateAfterSecondRun.get().getStateComment()).isNull();

        // Then: scores reflect the updated rule state
        var score = componentScoreRepository.findBySystemComponentAndDay(component, today);
        assertThat(score).isPresent();
        // 4 active rules, OK=30+10+40 (always-ok + exempted-rule + flipping-rule), total=100 -> 80%
        assertThat(score.get().getScore()).isEqualTo(80);

        // Then: conformance rates are updated (old rates deleted, new rates saved)
        ruleConformanceRateService.updateConformanceRates(secondResults, today);
        // flipping-rule: now OK for 1 component -> 100%
        assertConformanceRate("flipping-rule", today, 100);

        // Then: system conformance rates are updated
        ruleConformanceRateService.updateSystemConformanceRates(Map.of(system.getId(), secondResults), today);
        // flipping-rule: now OK for 1 component -> 1 conformant / 1 total = 100%
        assertSystemConformanceRate(system.getId(), "flipping-rule", today, 100);
    }

    private void assertConformanceRate(String ruleId, LocalDate day, int expectedRate) {
        var rate = ruleConformanceRateRepository.findByRuleIdAndDay(ruleId, day);
        assertThat(rate).as("Conformance rate for %s", ruleId).isPresent();
        assertThat(rate.get().getConformanceRate()).as("Conformance rate value for %s", ruleId).isEqualTo(expectedRate);
    }

    private void assertSystemConformanceRate(long systemId, String ruleId, LocalDate day, int expectedRate) {
        var rates = systemRuleConformanceRateRepository.findBySystemIdAndDay(systemId, day);
        var rate = rates.stream().filter(r -> r.getRuleId().equals(ruleId)).findFirst();
        assertThat(rate).as("System conformance rate for %s in system %d", ruleId, systemId).isPresent();
        assertThat(rate.get().getConformanceRate()).as("System conformance rate value for %s", ruleId).isEqualTo(expectedRate);
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
