package ch.admin.bit.jeap.governance.rules.core.dbschema;

import ch.admin.bit.jeap.governance.archrepo.domain.DatabaseSchemaVersion;
import ch.admin.bit.jeap.governance.archrepo.domain.DatabaseSchemaVersionRepository;
import ch.admin.bit.jeap.governance.deploymentlog.domain.DeploymentLogComponentVersion;
import ch.admin.bit.jeap.governance.deploymentlog.domain.DeploymentLogComponentVersionRepository;
import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleParameters;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleResult;
import ch.admin.bit.jeap.governance.domain.rule.State;
import ch.admin.bit.jeap.governance.prometheus.domain.PromQueryType;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeries;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeriesQueryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComponentPublishesDbSchemaRuleTest {

    @Mock
    private PromTimeSeriesQueryRepository promTimeSeriesQueryRepository;
    @Mock
    private DatabaseSchemaVersionRepository databaseSchemaVersionRepository;
    @Mock
    private DeploymentLogComponentVersionRepository deploymentLogComponentVersionRepository;

    @InjectMocks
    private ComponentPublishesDbSchemaRule rule;

    private final RuleParameters emptyParams = new RuleParameters(Map.of());

    @Test
    void metadata() {
        var metadata = rule.metadata();
        assertThat(metadata.ruleId().id()).isEqualTo("component-publishes-dbschema");
        assertThat(metadata.label()).isEqualTo("Component Publishes DB Schema");
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
        final String serviceName = "service-with-schema-without-version";
        SystemComponent component = mockComponent(1L, serviceName, componentType);

        //when
        RuleResult result = rule.evaluate(component, emptyParams);

        //then
        assertThat(result.state()).isEqualTo(State.OK);
        assertThat(result.stateComment()).isEqualTo("No database connections detected");
    }

    @ParameterizedTest
    @CsvSource({"mock-service", "foo-testagent-service", "foo-test-agent-service", "foo-test-agent"})
    void serviceIsIgnored_ruleStateIsOk(String name) {
        //given
        SystemComponent component = mockComponent(name);

        //when
        RuleResult result = rule.evaluate(component, new RuleParameters(Map.of("ignored-service-names", "mock,-testagent-service,-test-agent-service,-test-agent")));

        //then
        assertThat(result.state()).isEqualTo(State.OK);
        assertThat(result.stateComment()).isEqualTo("Component ignored for this rule");
    }

    @Test
    void serviceHasNoDatabase_ruleStateIsOk() {
        //given
        SystemComponent component = mockComponent(2L, "name", ComponentType.BACKEND_SERVICE);

        //when
        RuleResult result = rule.evaluate(component, emptyParams);

        //then
        assertThat(result.state()).isEqualTo(State.OK);
        assertThat(result.stateComment()).isEqualTo("No database connections detected");
    }

    @Test
    void serviceHasDatabaseSchemaWithoutVersion_ruleStateIsFail() {
        //given
        SystemComponent component = mockComponent(1L, "name", ComponentType.BACKEND_SERVICE);
        when(databaseSchemaVersionRepository.findByComponentId(1L)).thenReturn(Optional.empty());
        when(promTimeSeriesQueryRepository.findBy(PromQueryType.JDBC_CONNECTIONS_ACTIVE, 1L)).thenReturn(List.of(mock(PromTimeSeries.class)));

        //when
        RuleResult result = rule.evaluate(component, emptyParams);

        //then
        assertThat(result.state()).isEqualTo(State.FAIL);
        assertThat(result.stateComment()).isEqualTo("No database schema found in the architecture repository, it has to be uploaded by the application");
    }

    @Test
    void serviceHasDatabaseSchemaWithCurrentVersion_ruleStateIsOk() {
        //given
        SystemComponent component = mockComponent(1L, "name", ComponentType.BACKEND_SERVICE);
        final String version = "1.2.3";
        when(databaseSchemaVersionRepository.findByComponentId(1L)).thenReturn(Optional.of(DatabaseSchemaVersion.builder().version(version).build()));
        when(promTimeSeriesQueryRepository.findBy(PromQueryType.JDBC_CONNECTIONS_ACTIVE, 1L)).thenReturn(List.of(mock(PromTimeSeries.class)));
        when(deploymentLogComponentVersionRepository.findByComponentId(1L)).thenReturn(Optional.of(DeploymentLogComponentVersion.builder().version(version).build()));

        //when
        RuleResult result = rule.evaluate(component, emptyParams);

        //then
        assertThat(result.state()).isEqualTo(State.OK);
        assertThat(result.stateComment()).isEqualTo("Database schema in the architecture repository matches the currently deployed service version");
    }

    @Test
    void serviceHasDatabaseSchemaWithoutCurrentVersion_ruleStateIsOk() {
        //given
        SystemComponent component = mockComponent(1L, "name", ComponentType.BACKEND_SERVICE);
        final String version = "1.2.3";
        when(databaseSchemaVersionRepository.findByComponentId(1L)).thenReturn(Optional.of(DatabaseSchemaVersion.builder().version(version).build()));
        when(promTimeSeriesQueryRepository.findBy(PromQueryType.JDBC_CONNECTIONS_ACTIVE, 1L)).thenReturn(List.of(mock(PromTimeSeries.class)));
        when(deploymentLogComponentVersionRepository.findByComponentId(1L)).thenReturn(Optional.empty());

        //when
        RuleResult result = rule.evaluate(component, emptyParams);

        //then
        assertThat(result.state()).isEqualTo(State.OK);
        assertThat(result.stateComment()).isEqualTo("Database schema found in the architecture repository");
    }

    @Test
    void serviceHasDatabaseSchemaWithCurrentVersionThatNotMatches_ruleStateIsFail() {
        //given
        SystemComponent component = mockComponent(1L, "name", ComponentType.BACKEND_SERVICE);
        when(databaseSchemaVersionRepository.findByComponentId(1L)).thenReturn(Optional.of(DatabaseSchemaVersion.builder().version("1.2.3").build()));
        when(promTimeSeriesQueryRepository.findBy(PromQueryType.JDBC_CONNECTIONS_ACTIVE, 1L)).thenReturn(List.of(mock(PromTimeSeries.class)));
        when(deploymentLogComponentVersionRepository.findByComponentId(1L)).thenReturn(Optional.of(DeploymentLogComponentVersion.builder().version("foo").build()));

        //when
        RuleResult result = rule.evaluate(component, emptyParams);

        //then
        assertThat(result.state()).isEqualTo(State.FAIL);
        assertThat(result.stateComment()).isEqualTo("Database schema version '1.2.3' published to the architecture repository does not match the currently deployed service version 'foo'.");
    }

    private SystemComponent mockComponent(long id, String name, ComponentType type) {
        var component = org.mockito.Mockito.mock(SystemComponent.class);
        when(component.getId()).thenReturn(id);
        when(component.getName()).thenReturn(name);
        when(component.getType()).thenReturn(type);
        return component;
    }

    private SystemComponent mockComponent(String name) {
        var component = org.mockito.Mockito.mock(SystemComponent.class);
        when(component.getName()).thenReturn(name);
        when(component.getType()).thenReturn(ComponentType.BACKEND_SERVICE);
        return component;
    }

    private SystemComponent mockComponent(ComponentType type) {
        var component = org.mockito.Mockito.mock(SystemComponent.class);
        when(component.getType()).thenReturn(type);
        return component;
    }

}
