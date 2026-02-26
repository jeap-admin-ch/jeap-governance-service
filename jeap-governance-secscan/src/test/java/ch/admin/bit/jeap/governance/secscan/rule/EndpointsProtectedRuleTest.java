package ch.admin.bit.jeap.governance.secscan.rule;

import ch.admin.bit.jeap.governance.domain.GovernanceServiceEnvironment;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleParameters;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleResult;
import ch.admin.bit.jeap.governance.domain.rule.State;
import ch.admin.bit.jeap.governance.secscan.dataimport.DataimportConfigurationProperties;
import ch.admin.bit.jeap.governance.secscan.domain.SecscanFlaggedEndpoint;
import ch.admin.bit.jeap.governance.secscan.domain.SecscanFlaggedEndpointRepository;
import ch.admin.bit.jeap.governance.secscan.domain.SecscanState;
import ch.admin.bit.jeap.governance.secscan.domain.SecscanStateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EndpointsProtectedRuleTest {

    private static final String OK_MESSAGE = "No rest endpoint without proper protection detected";
    private static final String NO_SCAN_DATA_MESSAGE = "No security scan data available";
    private static final RuleParameters EMPTY_PARAMS = new RuleParameters(Map.of());

    @Mock
    private SecscanFlaggedEndpointRepository flaggedEndpointRepository;

    @Mock
    private SecscanStateRepository secscanStateRepository;

    @Mock
    private DataimportConfigurationProperties dataimportConfigurationProperties;

    @Mock
    private SystemComponent systemComponent;

    @InjectMocks
    private EndpointsProtectedRule rule;

    @Test
    void metadata() {
        assertThat(rule.metadata().ruleId().id()).isEqualTo("endpoints-protected");
        assertThat(rule.metadata().label()).isEqualTo("REST Endpoint Security (Scanner)");
    }

    @Test
    void noScanData_stateIsFail() {
        when(systemComponent.getId()).thenReturn(1L);
        when(systemComponent.getName()).thenReturn("test-service");
        when(secscanStateRepository.findBySystemComponentId(anyLong())).thenReturn(Optional.empty());

        RuleResult result = rule.evaluate(systemComponent, EMPTY_PARAMS);

        assertThat(result.state()).isEqualTo(State.FAIL);
        assertThat(result.stateComment()).isEqualTo(NO_SCAN_DATA_MESSAGE);
    }

    @Test
    void scanStateExistsButNoFlaggedEndpoints_stateIsOk() {
        when(systemComponent.getId()).thenReturn(1L);
        when(systemComponent.getName()).thenReturn("test-service");
        when(secscanStateRepository.findBySystemComponentId(anyLong())).thenReturn(Optional.of(createSecscanState(1L)));
        when(flaggedEndpointRepository.findBySystemComponentId(anyLong())).thenReturn(List.of());

        RuleResult result = rule.evaluate(systemComponent, EMPTY_PARAMS);

        assertThat(result.state()).isEqualTo(State.OK);
        assertThat(result.stateComment()).isEqualTo(OK_MESSAGE);
    }

    @Test
    void flaggedEndpointsExist_stateIsFail() {
        when(systemComponent.getId()).thenReturn(1L);
        when(systemComponent.getName()).thenReturn("test-service");
        when(secscanStateRepository.findBySystemComponentId(anyLong())).thenReturn(Optional.of(createSecscanState(1L)));
        when(flaggedEndpointRepository.findBySystemComponentId(anyLong())).thenReturn(List.of(
                createFlaggedEndpoint(1L, "/foo/bar", "GET", "HTTP 200 returned"),
                createFlaggedEndpoint(1L, "/bar/foo", "POST", "HTTP 200 returned")
        ));
        when(dataimportConfigurationProperties.getTargetEnvironment()).thenReturn(GovernanceServiceEnvironment.REF);

        RuleResult result = rule.evaluate(systemComponent, EMPTY_PARAMS);

        assertThat(result.state()).isEqualTo(State.FAIL);
        // Sorted alphabetically
        assertThat(result.stateComment()).contains("GET /foo/bar");
        assertThat(result.stateComment()).contains("POST /bar/foo");
    }

    @Test
    void flaggedEndpointsExist_allIgnoredByDefault_stateIsOk() {
        when(systemComponent.getId()).thenReturn(1L);
        when(systemComponent.getName()).thenReturn("test-service");
        when(secscanStateRepository.findBySystemComponentId(anyLong())).thenReturn(Optional.of(createSecscanState(1L)));
        when(flaggedEndpointRepository.findBySystemComponentId(anyLong())).thenReturn(List.of(
                createFlaggedEndpoint(1L, "/actuator/health", "GET", "HTTP 200 returned"),
                createFlaggedEndpoint(1L, "/actuator", "GET", "HTTP 200 returned"),
                createFlaggedEndpoint(1L, "/api-docs", "GET", "HTTP 200 returned"),
                createFlaggedEndpoint(1L, "/swagger-ui", "GET", "HTTP 200 returned")
        ));
        when(dataimportConfigurationProperties.getTargetEnvironment()).thenReturn(GovernanceServiceEnvironment.REF);

        RuleResult result = rule.evaluate(systemComponent, EMPTY_PARAMS);

        assertThat(result.state()).isEqualTo(State.OK);
        assertThat(result.stateComment()).isEqualTo(OK_MESSAGE);
    }

    @Test
    void flaggedEndpoints_apiDocsNotIgnoredOnNonRef_stateIsFail() {
        when(systemComponent.getId()).thenReturn(1L);
        when(systemComponent.getName()).thenReturn("test-service");
        when(secscanStateRepository.findBySystemComponentId(anyLong())).thenReturn(Optional.of(createSecscanState(1L)));
        when(flaggedEndpointRepository.findBySystemComponentId(anyLong())).thenReturn(List.of(
                createFlaggedEndpoint(1L, "/api-docs", "GET", "HTTP 200 returned"),
                createFlaggedEndpoint(1L, "/swagger-ui", "GET", "HTTP 200 returned")
        ));
        when(dataimportConfigurationProperties.getTargetEnvironment()).thenReturn(GovernanceServiceEnvironment.ABN);

        RuleResult result = rule.evaluate(systemComponent, EMPTY_PARAMS);

        assertThat(result.state()).isEqualTo(State.FAIL);
    }

    @Test
    void flaggedEndpoints_allIgnoredForSCS_stateIsOk() {
        when(systemComponent.getId()).thenReturn(1L);
        when(systemComponent.getName()).thenReturn("test-service");
        when(secscanStateRepository.findBySystemComponentId(anyLong())).thenReturn(Optional.of(createSecscanState(1L)));
        when(flaggedEndpointRepository.findBySystemComponentId(anyLong())).thenReturn(List.of(
                createFlaggedEndpoint(1L, "/**", "GET", null),
                createFlaggedEndpoint(1L, "/", "GET", null),
                createFlaggedEndpoint(1L, "/api/v3/configuration", "GET", null),
                createFlaggedEndpoint(1L, "/api/configuration/foo", "GET", null)
        ));
        when(dataimportConfigurationProperties.getTargetEnvironment()).thenReturn(GovernanceServiceEnvironment.REF);

        RuleResult result = rule.evaluate(systemComponent, EMPTY_PARAMS);

        assertThat(result.state()).isEqualTo(State.OK);
        assertThat(result.stateComment()).isEqualTo(OK_MESSAGE);
    }

    @Test
    void flaggedEndpoints_allIgnoredByExemptComponentNames_exactMatch_stateIsOk() {
        when(systemComponent.getName()).thenReturn("test-service");

        RuleParameters params = new RuleParameters(Map.of("exempt-component-names0", "test-service", "exempt-component-names1", "other-service"));
        RuleResult result = rule.evaluate(systemComponent, params);

        assertThat(result.state()).isEqualTo(State.OK);
        assertThat(result.stateComment()).isEqualTo(OK_MESSAGE);
    }

    @Test
    void flaggedEndpoints_allIgnoredByExemptComponentNames_prefixWildcard_stateIsOk() {
        when(systemComponent.getName()).thenReturn("test-service");

        RuleParameters params = new RuleParameters(Map.of("exempt-component-names0", "test-*"));
        RuleResult result = rule.evaluate(systemComponent, params);

        assertThat(result.state()).isEqualTo(State.OK);
    }

    @Test
    void flaggedEndpoints_allIgnoredByExemptComponentNames_suffixWildcard_stateIsOk() {
        when(systemComponent.getName()).thenReturn("test-service");

        RuleParameters params = new RuleParameters(Map.of("exempt-component-names0", "*-service"));
        RuleResult result = rule.evaluate(systemComponent, params);

        assertThat(result.state()).isEqualTo(State.OK);
    }

    @Test
    void flaggedEndpoints_exemptComponentNamesNoMatch_stateIsFail() {
        when(systemComponent.getId()).thenReturn(1L);
        when(systemComponent.getName()).thenReturn("test-service");
        when(secscanStateRepository.findBySystemComponentId(anyLong())).thenReturn(Optional.of(createSecscanState(1L)));
        when(flaggedEndpointRepository.findBySystemComponentId(anyLong())).thenReturn(List.of(
                createFlaggedEndpoint(1L, "/api/foo", "GET", "HTTP 200 returned")
        ));
        when(dataimportConfigurationProperties.getTargetEnvironment()).thenReturn(GovernanceServiceEnvironment.REF);

        RuleParameters params = new RuleParameters(Map.of("exempt-component-names0", "other-service"));
        RuleResult result = rule.evaluate(systemComponent, params);

        assertThat(result.state()).isEqualTo(State.FAIL);
    }

    @Test
    void flaggedEndpoints_allIgnoredByExemptEndpoints_stateIsOk() {
        when(systemComponent.getId()).thenReturn(1L);
        when(systemComponent.getName()).thenReturn("test-service");
        when(secscanStateRepository.findBySystemComponentId(anyLong())).thenReturn(Optional.of(createSecscanState(1L)));
        when(flaggedEndpointRepository.findBySystemComponentId(anyLong())).thenReturn(List.of(
                createFlaggedEndpoint(1L, "/api/foo", "GET", null),
                createFlaggedEndpoint(1L, "/api/bar", "GET", null),
                createFlaggedEndpoint(1L, "/foo/bar", "GET", null),
                createFlaggedEndpoint(1L, "/bar/foo", "GET", null)
        ));
        when(dataimportConfigurationProperties.getTargetEnvironment()).thenReturn(GovernanceServiceEnvironment.REF);

        RuleParameters params = new RuleParameters(Map.of("exempt-endpoints0", "GET:/api/*", "exempt-endpoints1", "GET:/foo/bar", "exempt-endpoints2", "GET:/bar/foo"));
        RuleResult result = rule.evaluate(systemComponent, params);

        assertThat(result.state()).isEqualTo(State.OK);
        assertThat(result.stateComment()).isEqualTo(OK_MESSAGE);
    }

    @Test
    void flaggedEndpoints_allIgnoredByExemptMethods_stateIsOk() {
        when(systemComponent.getId()).thenReturn(1L);
        when(systemComponent.getName()).thenReturn("test-service");
        when(secscanStateRepository.findBySystemComponentId(anyLong())).thenReturn(Optional.of(createSecscanState(1L)));
        when(flaggedEndpointRepository.findBySystemComponentId(anyLong())).thenReturn(List.of(
                createFlaggedEndpoint(1L, "/api/foo", "OPTIONS", null),
                createFlaggedEndpoint(1L, "/api/bar", "OPTIONS", null)
        ));
        when(dataimportConfigurationProperties.getTargetEnvironment()).thenReturn(GovernanceServiceEnvironment.REF);

        RuleParameters params = new RuleParameters(Map.of("exempt-methods0", "OPTIONS"));
        RuleResult result = rule.evaluate(systemComponent, params);

        assertThat(result.state()).isEqualTo(State.OK);
    }

    @Test
    void flaggedEndpoints_allIgnoredByExemptPaths_stateIsOk() {
        when(systemComponent.getId()).thenReturn(1L);
        when(systemComponent.getName()).thenReturn("test-service");
        when(secscanStateRepository.findBySystemComponentId(anyLong())).thenReturn(Optional.of(createSecscanState(1L)));
        when(flaggedEndpointRepository.findBySystemComponentId(anyLong())).thenReturn(List.of(
                createFlaggedEndpoint(1L, "/public/health", "GET", null),
                createFlaggedEndpoint(1L, "/public/info", "POST", null)
        ));
        when(dataimportConfigurationProperties.getTargetEnvironment()).thenReturn(GovernanceServiceEnvironment.REF);

        RuleParameters params = new RuleParameters(Map.of("exempt-paths0", "/public/*"));
        RuleResult result = rule.evaluate(systemComponent, params);

        assertThat(result.state()).isEqualTo(State.OK);
    }

    @Test
    void flaggedEndpoint_scanMessageIncluded() {
        when(systemComponent.getId()).thenReturn(1L);
        when(systemComponent.getName()).thenReturn("test-service");
        when(secscanStateRepository.findBySystemComponentId(anyLong())).thenReturn(Optional.of(createSecscanState(1L)));
        when(flaggedEndpointRepository.findBySystemComponentId(anyLong())).thenReturn(List.of(
                createFlaggedEndpoint(1L, "/api/test", "GET", "HTTP 200 returned")
        ));
        when(dataimportConfigurationProperties.getTargetEnvironment()).thenReturn(GovernanceServiceEnvironment.REF);

        RuleResult result = rule.evaluate(systemComponent, EMPTY_PARAMS);

        assertThat(result.state()).isEqualTo(State.FAIL);
        assertThat(result.stateComment()).contains("GET /api/test");
        assertThat(result.stateComment()).contains("HTTP 200 returned");
    }

    @Test
    void flaggedEndpoint_noScanMessage() {
        when(systemComponent.getId()).thenReturn(1L);
        when(systemComponent.getName()).thenReturn("test-service");
        when(secscanStateRepository.findBySystemComponentId(anyLong())).thenReturn(Optional.of(createSecscanState(1L)));
        when(flaggedEndpointRepository.findBySystemComponentId(anyLong())).thenReturn(List.of(
                createFlaggedEndpoint(1L, "/api/test", "GET", null)
        ));
        when(dataimportConfigurationProperties.getTargetEnvironment()).thenReturn(GovernanceServiceEnvironment.REF);

        RuleResult result = rule.evaluate(systemComponent, EMPTY_PARAMS);

        assertThat(result.state()).isEqualTo(State.FAIL);
        assertThat(result.stateComment()).isEqualTo("Endpoint 'GET /api/test' not properly protected");
    }

    @Test
    void multipleFlaggedEndpoints_resultsSorted() {
        when(systemComponent.getId()).thenReturn(1L);
        when(systemComponent.getName()).thenReturn("test-service");
        when(secscanStateRepository.findBySystemComponentId(anyLong())).thenReturn(Optional.of(createSecscanState(1L)));
        when(flaggedEndpointRepository.findBySystemComponentId(anyLong())).thenReturn(List.of(
                createFlaggedEndpoint(1L, "/z/path", "POST", null),
                createFlaggedEndpoint(1L, "/a/path", "GET", null)
        ));
        when(dataimportConfigurationProperties.getTargetEnvironment()).thenReturn(GovernanceServiceEnvironment.REF);

        RuleResult result = rule.evaluate(systemComponent, EMPTY_PARAMS);

        assertThat(result.state()).isEqualTo(State.FAIL);
        // Check sorted order: /a/path before /z/path
        assertThat(result.stateComment()).startsWith("Endpoint 'GET /a/path'");
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

    private static SecscanState createSecscanState(long systemComponentId) {
        return SecscanState.builder()
                .systemComponentId(systemComponentId)
                .scanMessage("Scan completed")
                .scanTimestamp(ZonedDateTime.now())
                .build();
    }

    private static SecscanFlaggedEndpoint createFlaggedEndpoint(long systemComponentId, String path, String method, String scanMessage) {
        return SecscanFlaggedEndpoint.builder()
                .systemComponentId(systemComponentId)
                .path(path)
                .method(method)
                .scanMessage(scanMessage)
                .scanTimestamp(ZonedDateTime.now())
                .build();
    }
}
