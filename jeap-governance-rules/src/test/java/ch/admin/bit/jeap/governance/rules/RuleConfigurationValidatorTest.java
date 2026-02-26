package ch.admin.bit.jeap.governance.rules;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.plugin.rule.Rule;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleMetadata;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleParameters;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleResult;
import ch.admin.bit.jeap.governance.domain.rule.RuleId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(OutputCaptureExtension.class)
class RuleConfigurationValidatorTest {

    @Test
    void unknownActiveRuleId_failOnError_throwsException(CapturedOutput output) {
        var properties = new RuleConfigurationProperties();
        var activeRule = new RuleConfigurationProperties.ActiveRule();
        activeRule.setId("non-existent-rule");
        properties.setActive(List.of(activeRule));

        var validator = new RuleConfigurationValidator(List.of(testRule("enforce-oauth2")), properties);

        assertThatThrownBy(() -> validator.validateConfiguration(true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unknown rule ID(s)");
        assertThat(output).contains("Active rule(s) reference unknown rule ID(s)")
                .contains("non-existent-rule");
    }

    @Test
    void unknownActiveRuleId_noFailOnError_logsErrorOnly(CapturedOutput output) {
        var properties = new RuleConfigurationProperties();
        var activeRule = new RuleConfigurationProperties.ActiveRule();
        activeRule.setId("non-existent-rule");
        properties.setActive(List.of(activeRule));

        var validator = new RuleConfigurationValidator(List.of(testRule("enforce-oauth2")), properties);

        validator.validateConfiguration(false);

        assertThat(output).contains("Active rule(s) reference unknown rule ID(s)")
                .contains("non-existent-rule");
    }

    @Test
    void unknownExemptionRuleId_failOnError_throwsException(CapturedOutput output) {
        var properties = new RuleConfigurationProperties();
        var activeRule = new RuleConfigurationProperties.ActiveRule();
        activeRule.setId("enforce-oauth2");
        properties.setActive(List.of(activeRule));
        var exemption = new RuleConfigurationProperties.ComponentExemption();
        exemption.setComponentName("some-service");
        exemption.setRuleId(List.of("non-existent-rule"));
        properties.setComponentExemptions(List.of(exemption));

        var validator = new RuleConfigurationValidator(List.of(testRule("enforce-oauth2")), properties);

        assertThatThrownBy(() -> validator.validateConfiguration(true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unknown rule ID(s)");
        assertThat(output).contains("Component exemption(s) reference unknown rule ID(s)")
                .contains("non-existent-rule");
    }

    @Test
    void unknownExemptionRuleId_noFailOnError_logsErrorOnly(CapturedOutput output) {
        var properties = new RuleConfigurationProperties();
        var activeRule = new RuleConfigurationProperties.ActiveRule();
        activeRule.setId("enforce-oauth2");
        properties.setActive(List.of(activeRule));
        var exemption = new RuleConfigurationProperties.ComponentExemption();
        exemption.setComponentName("some-service");
        exemption.setRuleId(List.of("non-existent-rule"));
        properties.setComponentExemptions(List.of(exemption));

        var validator = new RuleConfigurationValidator(List.of(testRule("enforce-oauth2")), properties);

        validator.validateConfiguration(false);

        assertThat(output).contains("Component exemption(s) reference unknown rule ID(s)")
                .contains("non-existent-rule");
    }

    @Test
    void allRuleIdsKnown_noErrorLogged(CapturedOutput output) {
        var properties = new RuleConfigurationProperties();
        var activeRule = new RuleConfigurationProperties.ActiveRule();
        activeRule.setId("enforce-oauth2");
        activeRule.setWeight(5);
        properties.setActive(List.of(activeRule));
        var exemption = new RuleConfigurationProperties.ComponentExemption();
        exemption.setComponentName("some-service");
        exemption.setRuleId(List.of("enforce-oauth2"));
        properties.setComponentExemptions(List.of(exemption));

        var validator = new RuleConfigurationValidator(List.of(testRule("enforce-oauth2")), properties);

        validator.validateConfiguration(true);

        assertThat(output).doesNotContain("unknown rule ID(s)");
    }

    @Test
    void validateParameters_delegatesToRule(CapturedOutput output) {
        var properties = new RuleConfigurationProperties();
        var activeRule = new RuleConfigurationProperties.ActiveRule();
        activeRule.setId("validating-rule");
        activeRule.setWeight(5);
        activeRule.setParameters(java.util.Map.of("key", "valid-value"));
        properties.setActive(List.of(activeRule));

        var validator = new RuleConfigurationValidator(List.of(testRuleWithValidation("validating-rule")), properties);

        validator.validateConfiguration(true);

        assertThat(output).doesNotContain("invalid parameters");
    }

    @Test
    void validateParameters_invalidParams_failOnError_throws(CapturedOutput output) {
        var properties = new RuleConfigurationProperties();
        var activeRule = new RuleConfigurationProperties.ActiveRule();
        activeRule.setId("validating-rule");
        activeRule.setWeight(5);
        activeRule.setParameters(java.util.Map.of("key", "invalid"));
        properties.setActive(List.of(activeRule));

        var validator = new RuleConfigurationValidator(List.of(testRuleWithValidation("validating-rule")), properties);

        assertThatThrownBy(() -> validator.validateConfiguration(true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("invalid value");
        assertThat(output).contains("Active rule 'validating-rule' has invalid parameters");
    }

    @Test
    void validateParameters_invalidParams_noFailOnError_logsOnly(CapturedOutput output) {
        var properties = new RuleConfigurationProperties();
        var activeRule = new RuleConfigurationProperties.ActiveRule();
        activeRule.setId("validating-rule");
        activeRule.setWeight(5);
        activeRule.setParameters(java.util.Map.of("key", "invalid"));
        properties.setActive(List.of(activeRule));

        var validator = new RuleConfigurationValidator(List.of(testRuleWithValidation("validating-rule")), properties);

        validator.validateConfiguration(false);

        assertThat(output).contains("Active rule 'validating-rule' has invalid parameters");
    }

    private static Rule testRule(String id) {
        return new Rule() {
            @Override
            public RuleMetadata metadata() {
                return new RuleMetadata(RuleId.of(id), "Rule " + id);
            }

            @Override
            public RuleResult evaluate(SystemComponent systemComponent, RuleParameters ruleParameters) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static Rule testRuleWithValidation(String id) {
        return new Rule() {
            @Override
            public RuleMetadata metadata() {
                return new RuleMetadata(RuleId.of(id), "Rule " + id);
            }

            @Override
            public RuleResult evaluate(SystemComponent systemComponent, RuleParameters ruleParameters) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void validateParameters(RuleParameters ruleParameters) {
                String value = ruleParameters.parameters().getOrDefault("key", "");
                if ("invalid".equals(value)) {
                    throw new IllegalStateException("invalid value for key");
                }
            }
        };
    }
}
