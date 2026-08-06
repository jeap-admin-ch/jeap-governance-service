package ch.admin.bit.jeap.governance.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ComponentTypeTest {

    @Test
    void isIgnoredForGovernance_handlesMissingAndKnownTypes() {
        assertThat(ComponentType.isIgnoredForGovernance(null)).isFalse();
        assertThat(ComponentType.isIgnoredForGovernance(ComponentType.UNKNOWN)).isFalse();
        assertThat(ComponentType.isIgnoredForGovernance(ComponentType.GATEWAY)).isTrue();
    }
}
