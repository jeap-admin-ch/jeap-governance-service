package ch.admin.bit.jeap.governance.rules;

import ch.admin.bit.jeap.governance.domain.rule.RuleId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {RuleConfigurationPropertiesTest.TestConfig.class, RefreshAutoConfiguration.class})
@ActiveProfiles("ruleconfig-test")
class RuleConfigurationPropertiesTest {

    @EnableConfigurationProperties(RuleConfigurationProperties.class)
    static class TestConfig {
    }

    @Autowired
    private RuleConfigurationProperties properties;

    @Test
    void activeRules_boundFromYaml() {
        assertThat(properties.getActive()).hasSize(3);
    }

    @Test
    void activeRule_simpleId() {
        var rule = properties.getActive().getFirst();

        assertThat(rule.getId()).isEqualTo(RuleId.of("enforce-oauth2"));
        assertThat(rule.getParameters()).isEmpty();
    }

    @Test
    void activeRule_withParameters() {
        var rule = properties.getActive().get(1);

        assertThat(rule.getId()).isEqualTo(RuleId.of("check-tls"));
        assertThat(rule.getParameters())
                .containsEntry("minVersion", "1.2")
                .containsEntry("timeout", "30");
    }

    @Test
    void activeRule_withSingleParameter() {
        var rule = properties.getActive().get(2);

        assertThat(rule.getId()).isEqualTo(RuleId.of("some-special-rule"));
        assertThat(rule.getParameters()).containsEntry("threshold", "10");
    }

    @Test
    void componentExemptions_boundFromYaml() {
        assertThat(properties.getComponentExemptions()).hasSize(2);
    }

    @Test
    void componentExemption_allFieldsBound() {
        var exemption = properties.getComponentExemptions().getFirst();

        assertThat(exemption.getId()).isEqualTo("legacy-auth-exemption");
        assertThat(exemption.getComponentName()).isEqualTo("old-service");
        assertThat(exemption.getRuleIds()).containsExactly(RuleId.of("enforce-oauth2"), RuleId.of("check-tls"));
        assertThat(exemption.getReason()).isEqualTo("Legacy service, uses basic auth");
        assertThat(exemption.getUntil()).isEqualTo(LocalDate.of(2026, 12, 24));
        assertThat(exemption.getParameters()).containsEntry("scope", "read");
    }

    @Test
    void componentExemption_withoutUntil_isNull() {
        var exemption = properties.getComponentExemptions().get(1);

        assertThat(exemption.getId()).isEqualTo("permanent-exemption");
        assertThat(exemption.getComponentName()).isEqualTo("internal-tool");
        assertThat(exemption.getRuleIds()).containsExactly(RuleId.of("enforce-oauth2"));
        assertThat(exemption.getReason()).isEqualTo("Internal tool, no external access");
        assertThat(exemption.getUntil()).isNull();
        assertThat(exemption.getParameters()).isEmpty();
    }
}
