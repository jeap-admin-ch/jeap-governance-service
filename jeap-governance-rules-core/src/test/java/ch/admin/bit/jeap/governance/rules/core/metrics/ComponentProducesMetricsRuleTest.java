package ch.admin.bit.jeap.governance.rules.core.metrics;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleParameters;
import ch.admin.bit.jeap.governance.domain.rule.State;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeriesQueryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComponentProducesMetricsRuleTest {

    @Mock
    private PromTimeSeriesQueryRepository repository;

    @InjectMocks
    private ComponentProducesMetricsRule rule;

    private final RuleParameters emptyParams = new RuleParameters(Map.of());

    @Test
    void metadata() {
        var metadata = rule.metadata();
        assertThat(metadata.ruleId().id()).isEqualTo("component-produces-metrics");
        assertThat(metadata.label()).isEqualTo("Component Produces Metrics");
    }

    @Test
    void componentWithMetrics_resultsInOk() {
        var component = mockComponent(1L);
        when(repository.anyTimeSeriesExistsBy(1L)).thenReturn(true);

        var result = rule.evaluate(component, emptyParams);

        assertThat(result.state()).isEqualTo(State.OK);
    }

    @Test
    void componentWithoutMetrics_resultsInFail() {
        var component = mockComponent(1L);
        when(component.getName()).thenReturn("my-component");
        when(repository.anyTimeSeriesExistsBy(1L)).thenReturn(false);

        var result = rule.evaluate(component, emptyParams);

        assertThat(result.state()).isEqualTo(State.FAIL);
        assertThat(result.stateComment()).contains("my-component");
        assertThat(result.stateComment()).contains("No prometheus metrics found");
    }

    private SystemComponent mockComponent(long id) {
        var component = org.mockito.Mockito.mock(SystemComponent.class);
        when(component.getId()).thenReturn(id);
        return component;
    }
}
