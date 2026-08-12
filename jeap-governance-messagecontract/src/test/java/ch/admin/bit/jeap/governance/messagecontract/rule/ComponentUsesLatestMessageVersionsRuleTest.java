package ch.admin.bit.jeap.governance.messagecontract.rule;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleParameters;
import ch.admin.bit.jeap.governance.domain.rule.State;
import ch.admin.bit.jeap.governance.messagecontract.domain.MessageContractVersionStatus;
import ch.admin.bit.jeap.governance.messagecontract.domain.MessageContractVersionStatusRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ComponentUsesLatestMessageVersionsRuleTest {
    private final MessageContractVersionStatusRepository repository = mock(MessageContractVersionStatusRepository.class);
    private final ComponentUsesLatestMessageVersionsRule rule = new ComponentUsesLatestMessageVersionsRule(repository);
    private final SystemComponent component = component(ComponentType.BACKEND_SERVICE);

    @Test
    void missingDataIsCompliant() {
        when(repository.findOutdatedByAppName("test-service")).thenReturn(List.of());

        assertThat(evaluate(component)).satisfies(result -> {
            assertThat(result.state()).isEqualTo(State.OK);
            assertThat(result.stateComment()).isEqualTo("No outdated message contracts found");
        });
    }

    @Test
    void failsWithSortedOutdatedContractDetails() {
        when(repository.findOutdatedByAppName("test-service")).thenReturn(List.of(
                status("ZEvent", "1.0.0", "2.0.0"), status("AEvent", "1.1.0", "1.2.0")));

        var result = evaluate(component);

        assertThat(result.state()).isEqualTo(State.FAIL);
        assertThat(result.stateComment()).contains("AEvent", "ZEvent", "1.1.0", "2.0.0")
                .containsSubsequence("AEvent", "ZEvent");
    }

    @Test
    void nonApplicableComponentDoesNotRequireImportedData() {
        assertThat(evaluate(component(ComponentType.FRONTEND))).extracting(r -> r.state()).isEqualTo(State.OK);
        verifyNoInteractions(repository);
    }

    private ch.admin.bit.jeap.governance.domain.plugin.rule.RuleResult evaluate(SystemComponent target) {
        return rule.evaluate(target, new RuleParameters(Map.of()));
    }

    private static SystemComponent component(ComponentType type) {
        return SystemComponent.builder().name("test-service").type(type).build();
    }

    private static MessageContractVersionStatus status(String messageType, String used, String latest) {
        return new MessageContractVersionStatus(
                "test-service", "1.0.0", messageType, used, latest, "topic", "CONSUMER", false);
    }
}
