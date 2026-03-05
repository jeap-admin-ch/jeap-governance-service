package ch.admin.bit.jeap.governance.rules.pact.cdc;

import ch.admin.bit.jeap.governance.archrepo.domain.RestApiRelationWithoutPact;
import ch.admin.bit.jeap.governance.archrepo.domain.RestApiRelationWithoutPactRepository;
import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleParameters;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleResult;
import ch.admin.bit.jeap.governance.domain.rule.State;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsumerContractWithinSystemRuleTest {

    @Mock
    private RestApiRelationWithoutPactRepository restApiRelationWithoutPactRepository;

    @InjectMocks
    private ConsumerContractWithinSystemRule rule;

    private final RuleParameters emptyParams = new RuleParameters(Map.of());

    @Test
    void metadata() {
        var metadata = rule.metadata();
        assertThat(metadata.ruleId().id()).isEqualTo("component-cdc-contractwithinsystem");
        assertThat(metadata.label()).isEqualTo("Consumer Contract Within System");
    }

    @Test
    void noRestRelationWithoutPact_ruleStateIsOk() {
        //given
        SystemComponent component = mockComponent(1L);
        List<RestApiRelationWithoutPact> restRelationsWithoutPact = List.of();
        when(restApiRelationWithoutPactRepository.findAllByConsumerSystemComponentId(1L)).thenReturn(restRelationsWithoutPact);

        //when
        RuleResult result = rule.evaluate(component, emptyParams);

        //then
        assertThat(result.state()).isEqualTo(State.OK);
        assertThat(result.stateComment()).isEqualTo("No rest relation without pact found");
    }

    @Test
    void restRelationWithoutPact_servicesBetweenSystem_ruleStateIsOk() {
        //given
        SystemComponent component = createComponent(1L, "test", mock(System.class));
        SystemComponent otherComponent = createComponent(2L, "test2", mock(System.class));
        List<RestApiRelationWithoutPact> restRelationsWithoutPact = List.of(
                RestApiRelationWithoutPact.builder().providerSystemComponent(otherComponent).consumerSystemComponent(component).method("GET").path("/foo/bar").build());
        when(restApiRelationWithoutPactRepository.findAllByConsumerSystemComponentId(1L)).thenReturn(restRelationsWithoutPact);

        //when
        RuleResult result = rule.evaluate(component, emptyParams);

        //then
        assertThat(result.state()).isEqualTo(State.OK);
        assertThat(result.stateComment()).isEqualTo("No rest relation without pact found");
    }

    @Test
    void restRelationWithoutPact_servicesWithinSystem_ruleStateIsFail() {
        //given
        System system = mock(System.class);
        SystemComponent component = createComponent(1L, "test", system);
        SystemComponent otherComponent = createComponent(2L, "test2", system);
        List<RestApiRelationWithoutPact> restRelationsWithoutPact = List.of(
                RestApiRelationWithoutPact.builder().providerSystemComponent(otherComponent).consumerSystemComponent(component).method("GET").path("/foo/bar").build());
        when(restApiRelationWithoutPactRepository.findAllByConsumerSystemComponentId(1L)).thenReturn(restRelationsWithoutPact);

        //when
        RuleResult result = rule.evaluate(component, emptyParams);

        //then
        assertThat(result.state()).isEqualTo(State.FAIL);
        assertThat(result.stateComment()).isEqualTo("No consumer contract found for relation with test2 on 'GET /foo/bar'");
    }

    @Test
    void restRelationWithoutPact_providerServiceIsIgnored_ruleStateIsOk() {
        //given
        System system = mock(System.class);
        SystemComponent component = createComponent(1L, "test", system);
        SystemComponent otherComponent1 = createComponent(2L, "otherComponent1", system);
        SystemComponent otherComponent2 = createComponent(3L, "otherComponent2", system);
        List<RestApiRelationWithoutPact> restRelationsWithoutPact = List.of(
                RestApiRelationWithoutPact.builder().providerSystemComponent(otherComponent1).consumerSystemComponent(component).method("GET").path("/foo/bar").build(),
                RestApiRelationWithoutPact.builder().providerSystemComponent(otherComponent2).consumerSystemComponent(component).method("GET").path("/foo/bar").build());
        when(restApiRelationWithoutPactRepository.findAllByConsumerSystemComponentId(1L)).thenReturn(restRelationsWithoutPact);

        //when
        RuleResult result = rule.evaluate(component, RuleParameters.ofList("services-to-ignore", List.of("otherComponent1", "otherComponent2")));

        //then
        assertThat(result.state()).isEqualTo(State.OK);
        assertThat(result.stateComment()).isEqualTo("No rest relation without pact found");
    }

    @Test
    void restRelationWithoutPact_consumerServiceIsIgnored_ruleStateIsOk() {
        //given
        System system = mock(System.class);
        SystemComponent component = createComponent(1L, "test", system);
        SystemComponent otherComponent1 = createComponent(2L, "otherComponent1", system);
        SystemComponent otherComponent2 = createComponent(3L, "otherComponent2", system);
        List<RestApiRelationWithoutPact> restRelationsWithoutPact = List.of(
                RestApiRelationWithoutPact.builder().providerSystemComponent(otherComponent1).consumerSystemComponent(component).method("GET").path("/foo/bar").build(),
                RestApiRelationWithoutPact.builder().providerSystemComponent(otherComponent2).consumerSystemComponent(component).method("GET").path("/foo/bar").build());
        when(restApiRelationWithoutPactRepository.findAllByConsumerSystemComponentId(1L)).thenReturn(restRelationsWithoutPact);

        //when
        RuleResult result = rule.evaluate(component, RuleParameters.ofList("services-to-ignore", List.of("test")));

        //then
        assertThat(result.state()).isEqualTo(State.OK);
        assertThat(result.stateComment()).isEqualTo("No rest relation without pact found");
    }

    @Test
    void restRelationWithoutPact_relationIsIgnored_ruleStateIsOk() {
        //given
        System system = mock(System.class);
        SystemComponent component = createComponent(1L, "test", system);
        SystemComponent otherComponent = createComponent(2L, "test2", system);
        List<RestApiRelationWithoutPact> restRelationsWithoutPact = List.of(
                RestApiRelationWithoutPact.builder().providerSystemComponent(otherComponent).consumerSystemComponent(component).method("GET").path("/foo/bar").build());
        when(restApiRelationWithoutPactRepository.findAllByConsumerSystemComponentId(1L)).thenReturn(restRelationsWithoutPact);

        //when
        RuleResult result = rule.evaluate(component, RuleParameters.ofList("relations-to-ignore", List.of("GET /foo/bar")));

        //then
        assertThat(result.state()).isEqualTo(State.OK);
        assertThat(result.stateComment()).isEqualTo("No rest relation without pact found");
    }

    @Test
    void allInvalid() {
        //given
        System system = mock(System.class);
        SystemComponent component = createComponent(1L, "test", system);
        SystemComponent otherComponent1 = createComponent(2L, "otherComponent1", system);
        SystemComponent otherComponent2 = createComponent(3L, "otherComponent2", system);
        List<RestApiRelationWithoutPact> restRelationsWithoutPact = List.of(
                RestApiRelationWithoutPact.builder().providerSystemComponent(otherComponent1).consumerSystemComponent(component).method("GET").path("/foo/bar1").build(),
                RestApiRelationWithoutPact.builder().providerSystemComponent(otherComponent2).consumerSystemComponent(component).method("POST").path("/foo/bar2").build());
        when(restApiRelationWithoutPactRepository.findAllByConsumerSystemComponentId(1L)).thenReturn(restRelationsWithoutPact);

        //when
        RuleResult result = rule.evaluate(component, emptyParams);

        //then
        assertThat(result.state()).isEqualTo(State.FAIL);
        assertThat(result.stateComment()).isEqualTo("No consumer contract found for relation with otherComponent1 on 'GET /foo/bar1'; No consumer contract found for relation with otherComponent2 on 'POST /foo/bar2'");
    }

    private SystemComponent createComponent(long id, String name, System system) {
        SystemComponent component = SystemComponent.builder().name(name)
                .type(ComponentType.SELF_CONTAINED_SYSTEM)
                .createdAt(ZonedDateTime.now()).build();
        ReflectionTestUtils.setField(component, "system", system);
        ReflectionTestUtils.setField(component, "id", id);
        return component;
    }

    private SystemComponent mockComponent(long id) {
        var component = mock(SystemComponent.class);
        when(component.getId()).thenReturn(id);
        return component;
    }
}
