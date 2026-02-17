package ch.admin.bit.jeap.governance.rules;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.rule.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static ch.admin.bit.jeap.governance.domain.rule.State.OK;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {RuleRepositoryImplTest.TestConfig.class, RefreshAutoConfiguration.class})
@ActiveProfiles("rule-repo-test")
class RuleRepositoryImplTest {

    @EnableConfigurationProperties(RuleConfigurationProperties.class)
    static class TestConfig {

        @Bean
        RuleRepositoryImpl ruleRepository(List<Rule> rules, RuleConfigurationProperties properties) {
            return new RuleRepositoryImpl(rules, properties);
        }

        @Bean
        Rule enforceOauth2Rule() {
            return new TestRule("enforce-oauth2", "Enforce OAuth2", 10);
        }

        @Bean
        Rule checkTlsRule() {
            return new TestRule("check-tls", "Check TLS", 5);
        }

        @Bean
        Rule unconfiguredRule() {
            return new TestRule("unconfigured-rule", "Unconfigured Rule", 1);
        }
    }

    @Autowired
    private RuleRepository ruleRepository;

    @Test
    void activeRule_returnsActiveState() {
        SystemComponent component = buildComponent("normal-service");

        List<RuleEvaluation> evaluations = ruleRepository.getRulesToEvaluateForComponent(component);

        RuleEvaluation oauth2 = findByRuleId(evaluations, RuleId.of("enforce-oauth2"));
        assertThat(oauth2.activationState()).isEqualTo(RuleActivationState.ACTIVE);
    }

    @Test
    void activeRuleWithParameters_returnsCorrectParameters() {
        SystemComponent component = buildComponent("normal-service");

        List<RuleEvaluation> evaluations = ruleRepository.getRulesToEvaluateForComponent(component);

        RuleEvaluation tls = findByRuleId(evaluations, RuleId.of("check-tls"));
        assertThat(tls.activationState()).isEqualTo(RuleActivationState.ACTIVE);
        assertThat(tls.ruleParameters().parameters())
                .containsEntry("minVersion", "1.2")
                .containsEntry("timeout", "30");
    }

    @Test
    void permanentExemption_returnsExemptedState() {
        SystemComponent component = buildComponent("exempted-service");

        List<RuleEvaluation> evaluations = ruleRepository.getRulesToEvaluateForComponent(component);

        RuleEvaluation oauth2 = findByRuleId(evaluations, RuleId.of("enforce-oauth2"));
        assertThat(oauth2.activationState()).isEqualTo(RuleActivationState.EXEMPTED);
    }

    @Test
    void temporaryExemption_returnsExemptedUntilState() {
        SystemComponent component = buildComponent("temporary-exempted-service");

        List<RuleEvaluation> evaluations = ruleRepository.getRulesToEvaluateForComponent(component);

        RuleEvaluation oauth2 = findByRuleId(evaluations, RuleId.of("enforce-oauth2"));
        assertThat(oauth2.activationState()).isEqualTo(RuleActivationState.EXEMPTED_UNTIL);
    }

    @Test
    void expiredExemption_returnsActiveState() {
        SystemComponent component = buildComponent("expired-exemption-service");

        List<RuleEvaluation> evaluations = ruleRepository.getRulesToEvaluateForComponent(component);

        RuleEvaluation oauth2 = findByRuleId(evaluations, RuleId.of("enforce-oauth2"));
        assertThat(oauth2.activationState()).isEqualTo(RuleActivationState.ACTIVE);
    }

    @Test
    void unconfiguredRule_notReturned() {
        SystemComponent component = buildComponent("normal-service");

        List<RuleEvaluation> evaluations = ruleRepository.getRulesToEvaluateForComponent(component);

        assertThat(evaluations).hasSize(2);
        assertThat(evaluations).noneMatch(e -> e.rule().metadata().ruleId().id().equals("unconfigured-rule"));
    }

    @Test
    void componentWithoutExemptions_allRulesActive() {
        SystemComponent component = buildComponent("normal-service");

        List<RuleEvaluation> evaluations = ruleRepository.getRulesToEvaluateForComponent(component);

        assertThat(evaluations).hasSize(2);
        assertThat(evaluations).allMatch(e -> e.activationState() == RuleActivationState.ACTIVE);
    }

    @Test
    void exemptionOnlyAppliesForMatchingRules() {
        SystemComponent component = buildComponent("exempted-service");

        List<RuleEvaluation> evaluations = ruleRepository.getRulesToEvaluateForComponent(component);

        RuleEvaluation oauth2 = findByRuleId(evaluations, RuleId.of("enforce-oauth2"));
        RuleEvaluation tls = findByRuleId(evaluations, RuleId.of("check-tls"));
        assertThat(oauth2.activationState()).isEqualTo(RuleActivationState.EXEMPTED);
        assertThat(tls.activationState()).isEqualTo(RuleActivationState.ACTIVE);
    }

    @Test
    void exemptionParameters_areMergedIntoRuleParameters() {
        SystemComponent component = buildComponent("exempted-service");

        List<RuleEvaluation> evaluations = ruleRepository.getRulesToEvaluateForComponent(component);

        RuleEvaluation oauth2 = findByRuleId(evaluations, RuleId.of("enforce-oauth2"));
        assertThat(oauth2.ruleParameters().parameters()).containsEntry("scope", "internal");
    }

    private static RuleEvaluation findByRuleId(List<RuleEvaluation> evaluations, RuleId ruleId) {
        return evaluations.stream()
                .filter(e -> e.rule().metadata().ruleId().equals(ruleId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No evaluation found for rule " + ruleId));
    }

    private static SystemComponent buildComponent(String name) {
        return SystemComponent.builder()
                .name(name)
                .state(OK)
                .type(ComponentType.BACKEND_SERVICE)
                .build();
    }

    static class TestRule implements Rule {

        private final RuleMetadata metadata;

        TestRule(String id, String label, int weight) {
            this.metadata = new RuleMetadata(RuleId.of(id), label, null, weight);
        }

        @Override
        public RuleMetadata metadata() {
            return metadata;
        }

        @Override
        public RuleEvaluationResult evaluate(SystemComponent systemComponent, RuleParameters ruleParameters) {
            throw new UnsupportedOperationException();
        }
    }
}
