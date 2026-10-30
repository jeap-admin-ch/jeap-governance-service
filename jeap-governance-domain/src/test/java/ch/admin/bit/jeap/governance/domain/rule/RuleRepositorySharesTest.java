package ch.admin.bit.jeap.governance.domain.rule;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RuleRepositorySharesTest {

    private final RuleRepository ruleRepository = mock(RuleRepository.class, CALLS_REAL_METHODS);

    @Test
    void threeActiveRules_shareTheTotalWeight() {
        when(ruleRepository.getActiveRuleWeights()).thenReturn(Map.of(
                RuleId.of("a"), 10, RuleId.of("b"), 5, RuleId.of("c"), 5));

        Map<RuleId, BigDecimal> shares = ruleRepository.getActiveRuleShares();

        assertThat(shares).isEqualTo(Map.of(
                RuleId.of("a"), new BigDecimal("50.0"),
                RuleId.of("b"), new BigDecimal("25.0"),
                RuleId.of("c"), new BigDecimal("25.0")));
        assertThat(shares.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("100");
    }

    @Test
    void sharesAreRoundedToOneDecimal() {
        when(ruleRepository.getActiveRuleWeights()).thenReturn(Map.of(
                RuleId.of("a"), 1, RuleId.of("b"), 2));

        assertThat(ruleRepository.getActiveRuleShares()).isEqualTo(Map.of(
                RuleId.of("a"), new BigDecimal("33.3"),
                RuleId.of("b"), new BigDecimal("66.7")));
    }

    @Test
    void withoutActiveRules_noShares() {
        when(ruleRepository.getActiveRuleWeights()).thenReturn(Map.of());

        assertThat(ruleRepository.getActiveRuleShares()).isEmpty();
    }
}
