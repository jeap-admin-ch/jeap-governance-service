package ch.admin.bit.jeap.governance.domain.rule;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RuleComponentExemptionTest {

    private static final LocalDate UNTIL = LocalDate.of(2026, 6, 30);

    @Test
    void activationState_beforeUntilDay_isExemptedUntil() {
        assertThat(exemption(UNTIL).activationState(UNTIL.minusDays(1))).isEqualTo(RuleActivationState.EXEMPTED_UNTIL);
    }

    @Test
    void activationState_onUntilDay_isActive() {
        assertThat(exemption(UNTIL).activationState(UNTIL)).isEqualTo(RuleActivationState.ACTIVE);
    }

    @Test
    void activationState_afterUntilDay_isActive() {
        assertThat(exemption(UNTIL).activationState(UNTIL.plusDays(1))).isEqualTo(RuleActivationState.ACTIVE);
    }

    @Test
    void activationState_withoutUntil_isExempted() {
        assertThat(exemption(null).activationState(UNTIL)).isEqualTo(RuleActivationState.EXEMPTED);
    }

    private static RuleComponentExemption exemption(LocalDate until) {
        return new RuleComponentExemption("exemption-1", "component", "reason", until, Map.of());
    }
}
