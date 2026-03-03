package ch.admin.bit.jeap.governance.rules;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.plugin.rule.Rule;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleMetadata;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleParameters;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleResult;
import ch.admin.bit.jeap.governance.domain.rule.RuleActivationState;
import ch.admin.bit.jeap.governance.domain.rule.RuleEvaluation;
import ch.admin.bit.jeap.governance.domain.rule.RuleId;
import ch.admin.bit.jeap.governance.domain.rule.RuleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

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
            return new TestRule("enforce-oauth2", "Enforce OAuth2");
        }

        @Bean
        Rule checkTlsRule() {
            return new TestRule("check-tls", "Check TLS");
        }

        @Bean
        Rule unconfiguredRule() {
            return new TestRule("unconfigured-rule", "Unconfigured Rule");
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

        assertThat(evaluations)
                .hasSize(2)
                .noneMatch(e -> e.rule().metadata().ruleId().id().equals("unconfigured-rule"));
    }

    @Test
    void componentWithoutExemptions_allRulesActive() {
        SystemComponent component = buildComponent("normal-service");

        List<RuleEvaluation> evaluations = ruleRepository.getRulesToEvaluateForComponent(component);

        assertThat(evaluations)
                .hasSize(2)
                .allMatch(e -> e.activationState() == RuleActivationState.ACTIVE);
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
                .type(ComponentType.BACKEND_SERVICE)
                .build();
    }

    static class TestRule implements Rule {

        private final RuleMetadata metadata;

        TestRule(String id, String label) {
            this.metadata = new RuleMetadata(RuleId.of(id), label);
        }

        @Override
        public RuleMetadata metadata() {
            return metadata;
        }

        @Override
        public RuleResult evaluate(SystemComponent systemComponent, RuleParameters ruleParameters) {
            throw new UnsupportedOperationException();
        }
    }
}
