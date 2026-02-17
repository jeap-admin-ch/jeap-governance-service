package ch.admin.bit.jeap.governance.rules;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.rule.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class RuleConfigurationValidatorTest {

    @Test
    void unknownActiveRuleId_logsError(CapturedOutput output) {
        var properties = new RuleConfigurationProperties();
        var activeRule = new RuleConfigurationProperties.ActiveRule();
        activeRule.setId("non-existent-rule");
        properties.setActive(List.of(activeRule));

        var validator = new RuleConfigurationValidator(List.of(testRule("enforce-oauth2")), properties);

        validator.validateConfiguration();

        assertThat(output).contains("Active rule(s) reference unknown rule ID(s)")
                .contains("non-existent-rule");
    }

    @Test
    void unknownExemptionRuleId_logsError(CapturedOutput output) {
        var properties = new RuleConfigurationProperties();
        var activeRule = new RuleConfigurationProperties.ActiveRule();
        activeRule.setId("enforce-oauth2");
        properties.setActive(List.of(activeRule));
        var exemption = new RuleConfigurationProperties.ComponentExemption();
        exemption.setComponentName("some-service");
        exemption.setRuleId(List.of("non-existent-rule"));
        properties.setComponentExemptions(List.of(exemption));

        var validator = new RuleConfigurationValidator(List.of(testRule("enforce-oauth2")), properties);

        validator.validateConfiguration();

        assertThat(output).contains("Component exemption(s) reference unknown rule ID(s)")
                .contains("non-existent-rule");
    }

    @Test
    void allRuleIdsKnown_noErrorLogged(CapturedOutput output) {
        var properties = new RuleConfigurationProperties();
        var activeRule = new RuleConfigurationProperties.ActiveRule();
        activeRule.setId("enforce-oauth2");
        properties.setActive(List.of(activeRule));
        var exemption = new RuleConfigurationProperties.ComponentExemption();
        exemption.setComponentName("some-service");
        exemption.setRuleId(List.of("enforce-oauth2"));
        properties.setComponentExemptions(List.of(exemption));

        var validator = new RuleConfigurationValidator(List.of(testRule("enforce-oauth2")), properties);

        validator.validateConfiguration();

        assertThat(output).doesNotContain("unknown rule ID(s)");
    }

    private static Rule testRule(String id) {
        return new Rule() {
            @Override
            public RuleMetadata metadata() {
                return new RuleMetadata(RuleId.of(id), "Rule " + id, null, 1);
            }

            @Override
            public RuleEvaluationResult evaluate(SystemComponent systemComponent, RuleParameters ruleParameters) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
