package ch.admin.bit.jeap.governance.rules;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.plugin.rule.Rule;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleInfo;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleMetadata;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleParameters;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleResult;
import ch.admin.bit.jeap.governance.domain.rule.RuleActivationState;
import ch.admin.bit.jeap.governance.domain.rule.RuleComponentExemption;
import ch.admin.bit.jeap.governance.domain.rule.RuleDescription;
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

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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
    void activeRuleInfo_containsConfiguredViolationDelay() {
        RuleInfo oauth2 = ruleRepository.getActiveRuleInfos().stream()
                .filter(ruleInfo -> ruleInfo.ruleId().equals(RuleId.of("enforce-oauth2")))
                .findFirst()
                .orElseThrow();

        assertThat(oauth2.violationDelay()).isEqualTo(Duration.ofDays(7));
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

    @Test
    void knownRules_containEveryRuleBeanSortedByRuleId() {
        assertThat(ruleRepository.getKnownRules())
                .extracting(rule -> rule.ruleId().id())
                .containsExactly("check-tls", "enforce-oauth2", "unconfigured-rule");
    }

    @Test
    void knownRules_describeAnActiveRuleAsConfigured() {
        RuleDescription checkTls = findKnownRule("check-tls");

        assertThat(checkTls.active()).isTrue();
        assertThat(checkTls.label()).isEqualTo("Check TLS");
        assertThat(checkTls.weight()).isEqualTo(5);
        assertThat(checkTls.violationDelay()).isEqualTo(Duration.ZERO);
        assertThat(checkTls.parameters()).isEqualTo(Map.of("minVersion", "1.2", "timeout", "30"));
    }

    @Test
    void knownRules_describeAnInactiveRuleWithoutConfiguration() {
        RuleDescription unconfigured = findKnownRule("unconfigured-rule");

        assertThat(unconfigured.active()).isFalse();
        assertThat(unconfigured.label()).isEqualTo("Unconfigured Rule");
        assertThat(unconfigured.weight()).isNull();
        assertThat(unconfigured.violationDelay()).isNull();
        assertThat(unconfigured.parameters()).isEmpty();
        assertThat(unconfigured.exemptions()).isEmpty();
    }

    @Test
    void knownRules_carryTheExplanationResourceUnchanged() {
        assertThat(findKnownRule("enforce-oauth2").explanation()).isEqualTo("""
                Checks that every endpoint requires an OAuth2 token.

                <b>Raw HTML</b> is passed on unchanged.
                """);
    }

    @Test
    void knownRules_ruleWithoutExplanationResource_hasNone() {
        assertThat(findKnownRule("check-tls").explanation()).isNull();
    }

    @Test
    void knownRules_listTheExemptionsNamingTheRule() {
        assertThat(findKnownRule("enforce-oauth2").exemptions())
                .extracting(RuleComponentExemption::id)
                .containsExactly("permanent-exemption", "temporary-exemption", "expired-exemption", "two-rules-exemption");
        assertThat(findKnownRule("enforce-oauth2").exemptions().getFirst()).isEqualTo(new RuleComponentExemption(
                "permanent-exemption", "exempted-service", "Internal tool, no external access", null,
                Map.of("scope", "internal")));
        assertThat(findKnownRule("enforce-oauth2").exemptions().get(1).until()).isEqualTo(LocalDate.of(2099, 12, 31));
    }

    @Test
    void knownRules_exemptionNamingTwoRules_isListedUnderBoth() {
        assertThat(findKnownRule("check-tls").exemptions())
                .extracting(RuleComponentExemption::id)
                .containsExactly("two-rules-exemption");
    }

    @Test
    void activeRuleShares_areComputedFromTheActiveWeights() {
        assertThat(ruleRepository.getActiveRuleShares()).isEqualTo(Map.of(
                RuleId.of("enforce-oauth2"), new BigDecimal("66.7"),
                RuleId.of("check-tls"), new BigDecimal("33.3")));
    }

    private RuleDescription findKnownRule(String ruleId) {
        return ruleRepository.getKnownRules().stream()
                .filter(rule -> rule.ruleId().equals(RuleId.of(ruleId)))
                .findFirst()
                .orElseThrow();
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
