package ch.admin.bit.jeap.governance.rules.core.security;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleParameters;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleResult;
import ch.admin.bit.jeap.governance.domain.rule.State;
import ch.admin.bit.jeap.governance.prometheus.domain.PromQueryType;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeries;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeriesQueryRepository;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeriesSample;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EndpointsProtectedByJwtRuleTest {

    private static final String OK_MESSAGE = "No rest endpoint without JWT bearer token protection detected";
    private static final RuleParameters EMPTY_PARAMS = new RuleParameters(Map.of());

    @Mock
    private PromTimeSeriesQueryRepository repository;

    @Mock
    private SystemComponent systemComponent;

    @InjectMocks
    private EndpointsProtectedByJwtRule rule;

    @Test
    void metadata() {
        assertThat(rule.metadata().ruleId().id()).isEqualTo("endpoints-protected-by-jwt");
        assertThat(rule.metadata().label()).isEqualTo("REST Endpoint Security (Monitoring)");
    }

    @Test
    void noRestEndpointFound_stateIsOk() {
        when(systemComponent.getId()).thenReturn(1L);
        when(repository.findBy(eq(PromQueryType.JEAP_REST_ENDPOINT_WITHOUT_JWT), anyLong())).thenReturn(List.of());

        RuleResult result = rule.evaluate(systemComponent, EMPTY_PARAMS);

        assertThat(result.state()).isEqualTo(State.OK);
        assertThat(result.stateComment()).isEqualTo(OK_MESSAGE);
    }

    @Test
    void restEndpointsFound_stateIsFail() {
        when(systemComponent.getId()).thenReturn(1L);
        when(systemComponent.getName()).thenReturn("ServiceName");
        when(repository.findBy(eq(PromQueryType.JEAP_REST_ENDPOINT_WITHOUT_JWT), anyLong())).thenReturn(List.of(
                timeSeries(Map.of("datapoint", "/bar/foo", "method", "POST")),
                timeSeries(Map.of("datapoint", "/foo/bar", "method", "GET"))
        ));

        RuleResult result = rule.evaluate(systemComponent, EMPTY_PARAMS);

        assertThat(result.state()).isEqualTo(State.FAIL);
        // Endpoints sorted alphabetically
        assertThat(result.stateComment()).isEqualTo(
                "Call 'GET /foo/bar' without a JWT bearer token detected on environment(s) 'unknown'; " +
                "Call 'POST /bar/foo' without a JWT bearer token detected on environment(s) 'unknown'");
    }

    @Test
    void restEndpointFound_groupedByEndpoint() {
        when(systemComponent.getId()).thenReturn(1L);
        when(systemComponent.getName()).thenReturn("ServiceName");
        when(repository.findBy(eq(PromQueryType.JEAP_REST_ENDPOINT_WITHOUT_JWT), anyLong())).thenReturn(List.of(
                timeSeries(Map.of("datapoint", "/foo/bar", "method", "GET", "stage", "ref")),
                timeSeries(Map.of("datapoint", "/foo/bar", "method", "GET", "stage", "prod"))
        ));

        RuleResult result = rule.evaluate(systemComponent, EMPTY_PARAMS);

        assertThat(result.state()).isEqualTo(State.FAIL);
        // Environments sorted alphabetically
        assertThat(result.stateComment()).isEqualTo(
                "Call 'GET /foo/bar' without a JWT bearer token detected on environment(s) 'prod, ref'");
    }

    @Test
    void restEndpointFound_allIgnoredByDefault_stateIsOk() {
        when(systemComponent.getId()).thenReturn(1L);
        when(systemComponent.getName()).thenReturn("ServiceName");
        when(repository.findBy(eq(PromQueryType.JEAP_REST_ENDPOINT_WITHOUT_JWT), anyLong())).thenReturn(List.of(
                timeSeries(Map.of("datapoint", "/actuator/health", "method", "GET", "stage", "ref")),
                timeSeries(Map.of("datapoint", "/actuator", "method", "GET", "stage", "ref")),
                timeSeries(Map.of("datapoint", "/api-docs", "method", "GET", "stage", "ref")),
                timeSeries(Map.of("datapoint", "/swagger-ui", "method", "GET", "stage", "ref"))
        ));

        RuleResult result = rule.evaluate(systemComponent, EMPTY_PARAMS);

        assertThat(result.state()).isEqualTo(State.OK);
    }

    @Test
    void restEndpointFound_notIgnoredByDefaultOnNonRef_stateIsFail() {
        when(systemComponent.getId()).thenReturn(1L);
        when(systemComponent.getName()).thenReturn("ServiceName");
        when(repository.findBy(eq(PromQueryType.JEAP_REST_ENDPOINT_WITHOUT_JWT), anyLong())).thenReturn(List.of(
                timeSeries(Map.of("datapoint", "/api-docs", "method", "GET")),
                timeSeries(Map.of("datapoint", "/swagger-ui", "method", "GET"))
        ));

        RuleResult result = rule.evaluate(systemComponent, EMPTY_PARAMS);

        assertThat(result.state()).isEqualTo(State.FAIL);
    }

    @Test
    void restEndpointFound_allIgnoredForSCS_stateIsOk() {
        when(systemComponent.getId()).thenReturn(1L);
        when(systemComponent.getName()).thenReturn("ServiceName");
        when(repository.findBy(eq(PromQueryType.JEAP_REST_ENDPOINT_WITHOUT_JWT), anyLong())).thenReturn(List.of(
                timeSeries(Map.of("datapoint", "/**", "method", "GET")),
                timeSeries(Map.of("datapoint", "/", "method", "GET")),
                timeSeries(Map.of("datapoint", "/api/v3/configuration", "method", "GET")),
                timeSeries(Map.of("datapoint", "/api/configuration/foo", "method", "GET"))
        ));

        RuleResult result = rule.evaluate(systemComponent, EMPTY_PARAMS);

        assertThat(result.state()).isEqualTo(State.OK);
    }

    @Test
    void restEndpointFound_allIgnoredByExemptComponentNames_exactMatch_stateIsOk() {
        when(systemComponent.getId()).thenReturn(1L);
        when(systemComponent.getName()).thenReturn("ServiceName");
        when(repository.findBy(eq(PromQueryType.JEAP_REST_ENDPOINT_WITHOUT_JWT), anyLong())).thenReturn(List.of(
                timeSeries(Map.of("datapoint", "/api/foo", "method", "GET")),
                timeSeries(Map.of("datapoint", "/api/bar", "method", "POST"))
        ));

        RuleParameters params = new RuleParameters(Map.of("exempt-component-names0", "ServiceName", "exempt-component-names1", "OtherService"));
        RuleResult result = rule.evaluate(systemComponent, params);

        assertThat(result.state()).isEqualTo(State.OK);
        assertThat(result.stateComment()).isEqualTo(OK_MESSAGE);
    }

    @Test
    void restEndpointFound_allIgnoredByExemptComponentNames_suffixWildcard_stateIsOk() {
        when(systemComponent.getId()).thenReturn(1L);
        when(systemComponent.getName()).thenReturn("my-test-service");
        when(repository.findBy(eq(PromQueryType.JEAP_REST_ENDPOINT_WITHOUT_JWT), anyLong())).thenReturn(List.of(
                timeSeries(Map.of("datapoint", "/api/foo", "method", "GET"))
        ));

        RuleParameters params = new RuleParameters(Map.of("exempt-component-names0", "*-service"));
        RuleResult result = rule.evaluate(systemComponent, params);

        assertThat(result.state()).isEqualTo(State.OK);
    }

    @Test
    void restEndpointFound_exemptComponentNamesNoMatch_stateIsFail() {
        when(systemComponent.getId()).thenReturn(1L);
        when(systemComponent.getName()).thenReturn("ServiceName");
        when(repository.findBy(eq(PromQueryType.JEAP_REST_ENDPOINT_WITHOUT_JWT), anyLong())).thenReturn(List.of(
                timeSeries(Map.of("datapoint", "/api/foo", "method", "GET"))
        ));

        RuleParameters params = new RuleParameters(Map.of("exempt-component-names0", "OtherService"));
        RuleResult result = rule.evaluate(systemComponent, params);

        assertThat(result.state()).isEqualTo(State.FAIL);
    }

    @Test
    void restEndpointFound_allIgnoredByExemptPaths_stateIsOk() {
        when(systemComponent.getId()).thenReturn(1L);
        when(systemComponent.getName()).thenReturn("ServiceName");
        when(repository.findBy(eq(PromQueryType.JEAP_REST_ENDPOINT_WITHOUT_JWT), anyLong())).thenReturn(List.of(
                timeSeries(Map.of("datapoint", "/api/foo", "method", "GET")),
                timeSeries(Map.of("datapoint", "/api/bar", "method", "GET")),
                timeSeries(Map.of("datapoint", "/foo/bar", "method", "GET")),
                timeSeries(Map.of("datapoint", "/bar/foo", "method", "GET"))
        ));

        RuleParameters params = new RuleParameters(Map.of("exempt-paths0", "/api/*", "exempt-paths1", "/foo/bar", "exempt-paths2", "/bar/foo"));
        RuleResult result = rule.evaluate(systemComponent, params);

        assertThat(result.state()).isEqualTo(State.OK);
    }

    @Test
    void restEndpointFound_allIgnoredByExemptMethods_stateIsOk() {
        when(systemComponent.getId()).thenReturn(1L);
        when(systemComponent.getName()).thenReturn("ServiceName");
        when(repository.findBy(eq(PromQueryType.JEAP_REST_ENDPOINT_WITHOUT_JWT), anyLong())).thenReturn(List.of(
                timeSeries(Map.of("datapoint", "/api/foo", "method", "OPTIONS")),
                timeSeries(Map.of("datapoint", "/api/bar", "method", "OPTIONS"))
        ));

        RuleParameters params = new RuleParameters(Map.of("exempt-methods0", "OPTIONS"));
        RuleResult result = rule.evaluate(systemComponent, params);

        assertThat(result.state()).isEqualTo(State.OK);
    }

    @Test
    void restEndpointFound_allIgnoredByExemptEndpoints_stateIsOk() {
        when(systemComponent.getId()).thenReturn(1L);
        when(systemComponent.getName()).thenReturn("ServiceName");
        when(repository.findBy(eq(PromQueryType.JEAP_REST_ENDPOINT_WITHOUT_JWT), anyLong())).thenReturn(List.of(
                timeSeries(Map.of("datapoint", "/api/foo", "method", "GET")),
                timeSeries(Map.of("datapoint", "/bar/baz", "method", "POST"))
        ));

        RuleParameters params = new RuleParameters(Map.of("exempt-endpoints0", "GET:/api/*", "exempt-endpoints1", "POST:/bar/baz"));
        RuleResult result = rule.evaluate(systemComponent, params);

        assertThat(result.state()).isEqualTo(State.OK);
    }

    // --- Parameter validation tests ---

    @Test
    void validateParameters_validParams_noException() {
        RuleParameters params = new RuleParameters(Map.of("exempt-endpoints0", "GET:/api/*"));

        assertThatNoException().isThrownBy(() -> rule.validateParameters(params));
    }

    @Test
    void validateParameters_emptyParams_noException() {
        assertThatNoException().isThrownBy(() -> rule.validateParameters(EMPTY_PARAMS));
    }

    @Test
    void validateParameters_invalidExemptEndpoints_throws() {
        RuleParameters params = new RuleParameters(Map.of("exempt-endpoints0", "/bad/path"));

        assertThatThrownBy(() -> rule.validateParameters(params))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("/bad/path");
    }

    private static PromTimeSeries timeSeries(Map<String, String> metric) {
        return PromTimeSeries.builder()
                .prometheusQueryType(PromQueryType.JEAP_REST_ENDPOINT_WITHOUT_JWT)
                .queryTimestamp(ZonedDateTime.now())
                .systemComponentId(1L)
                .sample(new PromTimeSeriesSample(metric, List.of("1")))
                .build();
    }
}
