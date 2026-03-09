package ch.admin.bit.jeap.governance.web;

import ch.admin.bit.jeap.governance.dataimport.DataImportScheduler;
import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.SystemRepository;
import ch.admin.bit.jeap.governance.domain.plugin.rule.Rule;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleMetadata;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleParameters;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleResult;
import ch.admin.bit.jeap.governance.domain.rule.RuleId;
import ch.admin.bit.jeap.governance.domain.rule.RuleState;
import ch.admin.bit.jeap.governance.domain.rule.State;
import ch.admin.bit.jeap.governance.domain.rule.SystemRuleConformanceRate;
import ch.admin.bit.jeap.governance.domain.score.ComponentScore;
import ch.admin.bit.jeap.governance.domain.score.SystemScore;
import ch.admin.bit.jeap.governance.rules.ScoringScheduler;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static ch.admin.bit.jeap.governance.web.ImportModelHelper.COMPONENT_B3_NAME;
import static ch.admin.bit.jeap.governance.web.ImportModelHelper.COMPONENT_C1_NAME;
import static ch.admin.bit.jeap.governance.web.ImportModelHelper.SYSTEM_B_NAME;
import static ch.admin.bit.jeap.governance.web.ImportModelHelper.SYSTEM_C_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("scoring-it")
@Import(ScoringWithImportIT.TestRuleConfig.class)
class ScoringWithImportIT extends GovernanceIntegrationTestBase {

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
    private DataImportScheduler dataImportScheduler;

    @Autowired
    private ScoringScheduler scoringScheduler;

    @Autowired
    private SystemRepository systemRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void deleteRuleStateAndComponentScore() throws Exception {
        FLIPPING_RULE_STATE.set(State.FAIL);
        setUpImportDefaultModel();
        dataImportScheduler.update();

        Optional<System> systemBOptional = systemRepository.findByName(SYSTEM_B_NAME);
        assertFalse(systemBOptional.isEmpty());
        System systemB = systemBOptional.get();

        scoringScheduler.updateScores();

        // We are only interested in the rule state and scores of component B3, which will be deleted in the next step
        SystemComponent componentB3 = findComponent(systemB, COMPONENT_B3_NAME);
        List<ComponentScore> componentScores = entityManager.createQuery("select c from ComponentScore c where c.systemComponent.id = :componentId", ComponentScore.class)
                .setParameter("componentId", componentB3.getId())
                .getResultList();
        assertEquals(1, componentScores.size());

        List<RuleState> ruleStates = entityManager.createQuery("select r from RuleState r where r.systemComponent.id = :componentId", RuleState.class)
                .setParameter("componentId", componentB3.getId())
                .getResultList();
        assertEquals(4, ruleStates.size());

        setUpImportModelLess();

        dataImportScheduler.update();

        componentScores = entityManager.createQuery("select c from ComponentScore c where c.systemComponent.id = :componentId", ComponentScore.class)
                .setParameter("componentId", componentB3.getId())
                .getResultList();
        assertTrue(componentScores.isEmpty());
        ruleStates = entityManager.createQuery("select r from RuleState r where r.systemComponent.id = :componentId", RuleState.class)
                .setParameter("componentId", componentB3.getId())
                .getResultList();
        assertTrue(ruleStates.isEmpty());
    }

    @Test
    void deleteSystem() throws Exception {
        FLIPPING_RULE_STATE.set(State.FAIL);
        setUpImportDefaultModel();
        dataImportScheduler.update();

        Optional<System> systemCOptional = systemRepository.findByName(SYSTEM_C_NAME);
        assertFalse(systemCOptional.isEmpty());
        System systemC = systemCOptional.get();

        scoringScheduler.updateScores();

        List<SystemScore> systemScores = entityManager.createQuery("select s from SystemScore s where s.system.id = :systemId", SystemScore.class)
                .setParameter("systemId", systemC.getId())
                .getResultList();
        assertEquals(1, systemScores.size());

        List<SystemRuleConformanceRate> systemRuleConformanceRates = entityManager.createQuery("select r from SystemRuleConformanceRate r where r.systemId = :systemId", SystemRuleConformanceRate.class)
                .setParameter("systemId", systemC.getId())
                .getResultList();
        assertEquals(4, systemRuleConformanceRates.size());

        SystemComponent componentC1 = findComponent(systemC, COMPONENT_C1_NAME);
        List<ComponentScore> componentScores = entityManager.createQuery("select c from ComponentScore c where c.systemComponent.id = :componentId", ComponentScore.class)
                .setParameter("componentId", componentC1.getId())
                .getResultList();
        assertEquals(1, componentScores.size());

        List<RuleState> ruleStates = entityManager.createQuery("select r from RuleState r where r.systemComponent.id = :componentId", RuleState.class)
                .setParameter("componentId", componentC1.getId())
                .getResultList();
        assertEquals(4, ruleStates.size());

        setUpImportModelLess();

        dataImportScheduler.update();

        systemScores = entityManager.createQuery("select s from SystemScore s where s.system.id = :systemId", SystemScore.class)
                .setParameter("systemId", systemC.getId())
                .getResultList();
        assertTrue(systemScores.isEmpty());

        systemRuleConformanceRates = entityManager.createQuery("select r from SystemRuleConformanceRate r where r.systemId = :systemId", SystemRuleConformanceRate.class)
                .setParameter("systemId", systemC.getId())
                .getResultList();
        assertTrue(systemRuleConformanceRates.isEmpty());

        componentScores = entityManager.createQuery("select c from ComponentScore c where c.systemComponent.id = :componentId", ComponentScore.class)
                .setParameter("componentId", componentC1.getId())
                .getResultList();
        assertTrue(componentScores.isEmpty());
        ruleStates = entityManager.createQuery("select r from RuleState r where r.systemComponent.id = :componentId", RuleState.class)
                .setParameter("componentId", componentC1.getId())
                .getResultList();
        assertTrue(ruleStates.isEmpty());
    }

    private static SystemComponent findComponent(System system, String name) {
        return system.getSystemComponents().stream()
                .filter(c -> c.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }
}
