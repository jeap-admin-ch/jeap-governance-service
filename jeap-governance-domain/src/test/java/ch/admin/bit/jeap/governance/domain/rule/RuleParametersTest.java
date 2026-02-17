package ch.admin.bit.jeap.governance.domain.rule;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RuleParametersTest {

    @Test
    void of_combinesRuleAndExemptionParameters() {
        var ruleParams = Map.of("threshold", "10", "timeout", "30");
        var exemptionParams = Map.of("scope", "read");

        var result = RuleParameters.of(ruleParams, exemptionParams);

        assertThat(result.parameters())
                .containsEntry("threshold", "10")
                .containsEntry("timeout", "30")
                .containsEntry("scope", "read");
    }

    @Test
    void of_exemptionParametersOverrideRuleParameters() {
        var ruleParams = Map.of("threshold", "10");
        var exemptionParams = Map.of("threshold", "99");

        var result = RuleParameters.of(ruleParams, exemptionParams);

        assertThat(result.parameters()).containsEntry("threshold", "99");
    }

    @Test
    void of_emptyExemptionParameters_returnsRuleParametersOnly() {
        var ruleParams = Map.of("threshold", "10");

        var result = RuleParameters.of(ruleParams, Map.of());

        assertThat(result.parameters()).containsExactlyEntriesOf(ruleParams);
    }

    @Test
    void of_emptyRuleParameters_returnsExemptionParametersOnly() {
        var exemptionParams = Map.of("scope", "read");

        var result = RuleParameters.of(Map.of(), exemptionParams);

        assertThat(result.parameters()).containsExactlyEntriesOf(exemptionParams);
    }

    @Test
    void of_bothEmpty_returnsEmptyParameters() {
        var result = RuleParameters.of(Map.of(), Map.of());

        assertThat(result.parameters()).isEmpty();
    }
}
