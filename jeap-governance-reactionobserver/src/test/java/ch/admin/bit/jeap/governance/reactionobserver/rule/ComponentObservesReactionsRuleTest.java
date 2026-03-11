package ch.admin.bit.jeap.governance.reactionobserver.rule;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleParameters;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleResult;
import ch.admin.bit.jeap.governance.domain.rule.State;
import ch.admin.bit.jeap.governance.prometheus.domain.PromQueryType;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeries;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeriesQueryRepository;
import ch.admin.bit.jeap.governance.reactionobserver.domain.ReactionObserverComponentLastObservationDate;
import ch.admin.bit.jeap.governance.reactionobserver.domain.ReactionObserverComponentLastObservationDateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComponentObservesReactionsRuleTest {

    @Mock
    private PromTimeSeriesQueryRepository promTimeSeriesQueryRepository;

    @Mock
    private ReactionObserverComponentLastObservationDateRepository lastObservationDateRepository;

    @InjectMocks
    private ComponentObservesReactionsRule rule;

    private final RuleParameters emptyParams = new RuleParameters(Map.of());

    @Test
    void metadata() {
        var metadata = rule.metadata();
        assertThat(metadata.ruleId().id()).isEqualTo("component-observes-reactions");
        assertThat(metadata.label()).isEqualTo("Component Observes Reactions");
    }

    @ParameterizedTest
    @EnumSource(value = ComponentType.class, names = {"FRONTEND", "MOBILE_APP", "UNKNOWN"})
    void returnsOkWhenServiceTypeIsNotApplicable(ComponentType componentType) {
        //given
        SystemComponent systemComponent = mockComponent(componentType);

        //when
        RuleResult result = rule.evaluate(systemComponent, emptyParams);

        //then
        assertThat(result.state()).isEqualTo(State.OK);
        assertThat(result.stateComment()).isEqualTo("Not applicable");
    }

    @ParameterizedTest
    @EnumSource(value = ComponentType.class, names = {"BACKEND_SERVICE", "SELF_CONTAINED_SYSTEM"})
    void returnsOkWhenNoMessaging(ComponentType componentType) {
        //given
        SystemComponent systemComponent = mockComponent(componentType);

        //when
        RuleResult result = rule.evaluate(systemComponent, emptyParams);

        //then
        assertThat(result.state()).isEqualTo(State.OK);
        assertThat(result.stateComment()).isEqualTo("No messaging library detected");
    }

    @ParameterizedTest
    @CsvSource({"mock-service", "foo-testagent-service", "foo-test-agent-service", "foo-test-agent"})
    void serviceIsIgnored_ruleStateIsOk(String name) {
        //given
        SystemComponent component = mockComponent(name);

        //when
        RuleResult result = rule.evaluate(component, RuleParameters.ofList("ignored-service-names", List.of("mock","-testagent-service","-test-agent-service","-test-agent")));

        //then
        assertThat(result.state()).isEqualTo(State.OK);
        assertThat(result.stateComment()).isEqualTo("Component ignored for this rule");
    }

    @Test
    void returnsOkWhenComponentWithoutMessaging() {
        //given
        SystemComponent systemComponent = mockComponent(1L, "my-service", ComponentType.BACKEND_SERVICE);

        //when
        RuleResult result = rule.evaluate(systemComponent, emptyParams);

        //then
        assertThat(result.state()).isEqualTo(State.OK);
        assertThat(result.stateComment()).isEqualTo("No messaging library detected");
    }


    @Test
    void returnsFailWhenComponentDoesNotObserveReactions() {
        //given
        SystemComponent systemComponent = mockComponent(2L, "my-service-2", ComponentType.SELF_CONTAINED_SYSTEM);

        when(promTimeSeriesQueryRepository.findBy(PromQueryType.JEAP_MESSAGING_TOTAL, 2L)).thenReturn(List.of(mock(PromTimeSeries.class)));

        //when
        RuleResult result = rule.evaluate(systemComponent, emptyParams);

        //then
        assertThat(result.state()).isEqualTo(State.FAIL);
        assertThat(result.stateComment()).isEqualTo("Component does not have a reaction graph and therefore does not observe reactions");
    }

    @Test
    void returnsFailWhenComponentDoesNotObserveReactionsFrom7Days() {
        //given
        SystemComponent systemComponent = mockComponent(1L, "my-service", ComponentType.BACKEND_SERVICE);

        when(promTimeSeriesQueryRepository.findBy(PromQueryType.JEAP_MESSAGING_TOTAL, 1L)).thenReturn(List.of(mock(PromTimeSeries.class)));
        ReactionObserverComponentLastObservationDate lastObservationDate = mock(ReactionObserverComponentLastObservationDate.class);
        when(lastObservationDate.getLastObservationDate()).thenReturn(LocalDate.of(2022,5,22));
        when(lastObservationDateRepository.findByComponentId(1L)).thenReturn(Optional.of(lastObservationDate));

        //when
        RuleResult result = rule.evaluate(systemComponent, emptyParams);

        //then
        assertThat(result.state()).isEqualTo(State.FAIL);
        assertThat(result.stateComment()).isEqualTo("Component has not observed any reactions in the last 7 days. Last observation date: 2022-05-22");
    }

    @Test
    void returnsOkWhenComponentObservesReactions() {
        //given
        SystemComponent systemComponent = mockComponent(1L, "my-service", ComponentType.BACKEND_SERVICE);

        when(promTimeSeriesQueryRepository.findBy(PromQueryType.JEAP_MESSAGING_TOTAL, 1L)).thenReturn(List.of(mock(PromTimeSeries.class)));
        ReactionObserverComponentLastObservationDate lastObservationDate = mock(ReactionObserverComponentLastObservationDate.class);
        when(lastObservationDate.getLastObservationDate()).thenReturn(LocalDate.now());
        when(lastObservationDateRepository.findByComponentId(1L)).thenReturn(Optional.of(lastObservationDate));

        //when
        RuleResult result = rule.evaluate(systemComponent, emptyParams);

        //then
        assertThat(result.state()).isEqualTo(State.OK);
        assertThat(result.stateComment()).isEqualTo("Component observes reactions. Last observation date: " + LocalDate.now());
    }

    @Test
    void returnsFailWhenComponentDoesNotObserveReactionsFromMaxDelayInDays() {
        //given
        SystemComponent systemComponent = mockComponent(1L, "my-service", ComponentType.BACKEND_SERVICE);
        LocalDate observationDate = LocalDate.now().minusDays(3);
        when(promTimeSeriesQueryRepository.findBy(PromQueryType.JEAP_MESSAGING_TOTAL, 1L)).thenReturn(List.of(mock(PromTimeSeries.class)));
        ReactionObserverComponentLastObservationDate lastObservationDate = mock(ReactionObserverComponentLastObservationDate.class);
        when(lastObservationDate.getLastObservationDate()).thenReturn(observationDate);
        when(lastObservationDateRepository.findByComponentId(1L)).thenReturn(Optional.of(lastObservationDate));

        //when
        RuleResult result = rule.evaluate(systemComponent, RuleParameters.of(Map.of(ComponentObservesReactionsRule.KEY_OBSERVATION_MAX_DELAY_IN_DAYS, "2"), Map.of()));

        //then
        assertThat(result.state()).isEqualTo(State.FAIL);
        assertThat(result.stateComment()).isEqualTo("Component has not observed any reactions in the last 2 days. Last observation date: " + observationDate);
    }


    private SystemComponent mockComponent(long id, String name, ComponentType type) {
        var component = mock(SystemComponent.class);
        when(component.getId()).thenReturn(id);
        when(component.getName()).thenReturn(name);
        when(component.getType()).thenReturn(type);
        return component;
    }

    private SystemComponent mockComponent(String name) {
        var component = mock(SystemComponent.class);
        when(component.getName()).thenReturn(name);
        when(component.getType()).thenReturn(ComponentType.BACKEND_SERVICE);
        return component;
    }

    private SystemComponent mockComponent(ComponentType type) {
        var component = mock(SystemComponent.class);
        when(component.getType()).thenReturn(type);
        return component;
    }
}
