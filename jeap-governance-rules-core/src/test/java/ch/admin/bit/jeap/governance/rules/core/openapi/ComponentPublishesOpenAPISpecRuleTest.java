package ch.admin.bit.jeap.governance.rules.core.openapi;

import ch.admin.bit.jeap.governance.archrepo.domain.ApiDocVersion;
import ch.admin.bit.jeap.governance.archrepo.domain.ApiDocVersionRepository;
import ch.admin.bit.jeap.governance.deploymentlog.domain.DeploymentLogComponentVersion;
import ch.admin.bit.jeap.governance.deploymentlog.domain.DeploymentLogComponentVersionRepository;
import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleParameters;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleResult;
import ch.admin.bit.jeap.governance.domain.rule.State;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComponentPublishesOpenAPISpecRuleTest {

    @Mock
    private ApiDocVersionRepository apiDocVersionRepository;
    @Mock
    private DeploymentLogComponentVersionRepository deploymentLogComponentVersionRepository;

    @InjectMocks
    private ComponentPublishesOpenAPISpecRule rule;

    private final RuleParameters emptyParams = new RuleParameters(Map.of());

    @Test
    void metadata() {
        var metadata = rule.metadata();
        assertThat(metadata.ruleId().id()).isEqualTo("component-publishes-openapispec");
        assertThat(metadata.label()).isEqualTo("Component Publishes OpenAPI Spec");
    }

    @ParameterizedTest
    @EnumSource(value = ComponentType.class, names = {"FRONTEND", "MOBILE_APP", "UNKNOWN"})
    void serviceHasNoBackend_ruleSkipped(ComponentType componentType) {
        //given
        SystemComponent component = mockComponent(componentType);

        //when
        RuleResult result = rule.evaluate(component, emptyParams);

        //then
        assertThat(result.state()).isEqualTo(State.OK);
        assertThat(result.stateComment()).isEqualTo("Not applicable");
    }

    @ParameterizedTest
    @EnumSource(value = ComponentType.class, names = {"BACKEND_SERVICE", "SELF_CONTAINED_SYSTEM"})
    void serviceHasBackend_ruleEvaluated(ComponentType componentType) {
        //given
        SystemComponent component = mockComponent(1L, componentType);

        //when
        RuleResult result = rule.evaluate(component, emptyParams);

        //then
        assertThat(result.state()).isEqualTo(State.FAIL);
        assertThat(result.stateComment()).isEqualTo("No OpenAPI specification found in the architecture repository, it has to be uploaded by the application");
    }


    @Test
    void serviceHasOpenAPISpecWithoutVersion_ruleStateIsFail() {
        //given
        SystemComponent component = mockComponent(1L, ComponentType.BACKEND_SERVICE);
        when(apiDocVersionRepository.findByComponentId(1L)).thenReturn(Optional.empty());

        //when
        RuleResult result = rule.evaluate(component, emptyParams);

        //then
        assertThat(result.state()).isEqualTo(State.FAIL);
        assertThat(result.stateComment()).isEqualTo("No OpenAPI specification found in the architecture repository, it has to be uploaded by the application");
    }

    @Test
    void serviceHasOpenAPISpecWithCurrentVersion_ruleStateIsOk() {
        //given
        SystemComponent component = mockComponent(1L, ComponentType.BACKEND_SERVICE);
        final String version = "1.2.3";
        when(apiDocVersionRepository.findByComponentId(1L)).thenReturn(Optional.of(ApiDocVersion.builder().version(version).build()));
        when(deploymentLogComponentVersionRepository.findByComponentId(1L)).thenReturn(Optional.of(DeploymentLogComponentVersion.builder().version(version).build()));

        //when
        RuleResult result = rule.evaluate(component, emptyParams);

        //then
        assertThat(result.state()).isEqualTo(State.OK);
        assertThat(result.stateComment()).isEqualTo("OpenAPI specification in the architecture repository matches the currently deployed service version");
    }

    @Test
    void serviceHasOpenAPISpecWithoutCurrentVersion_ruleStateIsOk() {
        //given
        SystemComponent component = mockComponent(2L, ComponentType.BACKEND_SERVICE);
        final String version = "1.2.3";
        when(apiDocVersionRepository.findByComponentId(2L)).thenReturn(Optional.of(ApiDocVersion.builder().version(version).build()));
        when(deploymentLogComponentVersionRepository.findByComponentId(2L)).thenReturn(Optional.empty());

        //when
        RuleResult result = rule.evaluate(component, emptyParams);

        //then
        assertThat(result.state()).isEqualTo(State.OK);
        assertThat(result.stateComment()).isEqualTo("OpenAPI specification found in the architecture repository");
    }

    @Test
    void serviceHasOpenAPISpecWithCurrentVersionThatNotMatches_ruleStateIsFail() {
        //given
        SystemComponent component = mockComponent(1L, ComponentType.BACKEND_SERVICE);
        when(apiDocVersionRepository.findByComponentId(1L)).thenReturn(Optional.of(ApiDocVersion.builder().version("1.2.3").build()));
        when(deploymentLogComponentVersionRepository.findByComponentId(1L)).thenReturn(Optional.of(DeploymentLogComponentVersion.builder().version("foo").build()));

        //when
        RuleResult result = rule.evaluate(component, emptyParams);

        //then
        assertThat(result.state()).isEqualTo(State.FAIL);
        assertThat(result.stateComment()).isEqualTo("OpenAPI specification version '1.2.3' published to the architecture repository does not match the currently deployed service version 'foo'.");
    }

    private SystemComponent mockComponent(long id, ComponentType type) {
        var component = org.mockito.Mockito.mock(SystemComponent.class);
        when(component.getId()).thenReturn(id);
        when(component.getType()).thenReturn(type);
        return component;
    }

    private SystemComponent mockComponent(ComponentType type) {
        var component = org.mockito.Mockito.mock(SystemComponent.class);
        when(component.getType()).thenReturn(type);
        return component;
    }

}
