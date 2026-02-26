package ch.admin.bit.jeap.governance.secscan.filter;

import ch.admin.bit.jeap.governance.domain.GovernanceServiceEnvironment;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.SystemComponentRepository;
import ch.admin.bit.jeap.governance.domain.plugin.rule.Rule;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleMetadata;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleParameters;
import ch.admin.bit.jeap.governance.domain.rule.RuleActivationState;
import ch.admin.bit.jeap.governance.domain.rule.RuleEvaluation;
import ch.admin.bit.jeap.governance.domain.rule.RuleId;
import ch.admin.bit.jeap.governance.domain.rule.RuleRepository;
import ch.admin.bit.jeap.governance.domain.plugin.security.api.HttpApi;
import ch.admin.bit.jeap.governance.domain.plugin.security.api.HttpEndpoint;
import ch.admin.bit.jeap.governance.domain.plugin.security.api.SystemComponentHttpApi;
import ch.admin.bit.jeap.governance.secscan.domain.SystemComponentHttpApiIgnoreFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigurableSystemComponentHttpApiIgnoreFilterTest {

    private static final String COMPONENT_NAME = "test-service";

    @Mock
    private SystemComponentRepository systemComponentRepository;

    @Mock
    private RuleRepository ruleRepository;

    @InjectMocks
    private ConfigurableSystemComponentHttpApiIgnoreFilter filter;

    // --- shouldIgnoreApi tests ---

    @Test
    void shouldIgnoreApi_componentNotFound_notIgnored() {
        when(systemComponentRepository.findByName(COMPONENT_NAME)).thenReturn(Optional.empty());

        SystemComponentHttpApiIgnoreFilter.Result result = filter.shouldIgnoreApi(createApi(COMPONENT_NAME, GovernanceServiceEnvironment.REF, "https://test-ref.ingress.foo.bar"));

        assertThat(result.ignore()).isFalse();
    }

    @Test
    void shouldIgnoreApi_ruleNotConfigured_notIgnored() {
        setupComponentAndRules(COMPONENT_NAME, List.of());

        SystemComponentHttpApiIgnoreFilter.Result result = filter.shouldIgnoreApi(createApi(COMPONENT_NAME, GovernanceServiceEnvironment.REF, "https://test-ref.ingress.foo.bar"));

        assertThat(result.ignore()).isFalse();
    }

    @Test
    void shouldIgnoreApi_ruleNotActive_notIgnored() {
        setupComponentAndRules(COMPONENT_NAME, List.of(
                createRuleEvaluation(RuleActivationState.EXEMPTED, Map.of())
        ));

        SystemComponentHttpApiIgnoreFilter.Result result = filter.shouldIgnoreApi(createApi(COMPONENT_NAME, GovernanceServiceEnvironment.REF, "https://test-ref.ingress.foo.bar"));

        assertThat(result.ignore()).isFalse();
    }

    @Test
    void shouldIgnoreApi_exemptEnvironment_ignored() {
        setupComponentAndRules(COMPONENT_NAME, List.of(
                createRuleEvaluation(RuleActivationState.ACTIVE, Map.of("exempt-environments0", "ABN", "exempt-environments1", "PROD"))
        ));

        SystemComponentHttpApiIgnoreFilter.Result result = filter.shouldIgnoreApi(createApi(COMPONENT_NAME, GovernanceServiceEnvironment.ABN, "https://test-abn.ingress.foo.bar"));

        assertThat(result.ignore()).isTrue();
        assertThat(result.reason()).contains("exempt-environments");
    }

    @Test
    void shouldIgnoreApi_nonExemptEnvironment_notIgnored() {
        setupComponentAndRules(COMPONENT_NAME, List.of(
                createRuleEvaluation(RuleActivationState.ACTIVE, Map.of("exempt-environments0", "ABN", "exempt-environments1", "PROD"))
        ));

        SystemComponentHttpApiIgnoreFilter.Result result = filter.shouldIgnoreApi(createApi(COMPONENT_NAME, GovernanceServiceEnvironment.REF, "https://test-ref.ingress.foo.bar"));

        assertThat(result.ignore()).isFalse();
    }

    @Test
    void shouldIgnoreApi_urlNotContaining_ignored() {
        setupComponentAndRules(COMPONENT_NAME, List.of(
                createRuleEvaluation(RuleActivationState.ACTIVE, Map.of("exempt-api-url-not-containing0", "ingress", "exempt-api-url-not-containing1", "internal-csp"))
        ));

        // URL does NOT contain "ingress" or "internal-csp" -> exempt
        SystemComponentHttpApiIgnoreFilter.Result result = filter.shouldIgnoreApi(createApi(COMPONENT_NAME, GovernanceServiceEnvironment.REF, "https://test-ref.foo.bar"));

        assertThat(result.ignore()).isTrue();
        assertThat(result.reason()).contains("exempt-api-url-not-containing");
    }

    @Test
    void shouldIgnoreApi_urlContainingIngress_notIgnored() {
        setupComponentAndRules(COMPONENT_NAME, List.of(
                createRuleEvaluation(RuleActivationState.ACTIVE, Map.of("exempt-api-url-not-containing0", "ingress", "exempt-api-url-not-containing1", "internal-csp"))
        ));

        // URL DOES contain "ingress" -> not exempt
        SystemComponentHttpApiIgnoreFilter.Result result = filter.shouldIgnoreApi(createApi(COMPONENT_NAME, GovernanceServiceEnvironment.REF, "https://test-ref.ingress.foo.bar"));

        assertThat(result.ignore()).isFalse();
    }

    @Test
    void shouldIgnoreApi_urlContainingInternalCsp_notIgnored() {
        setupComponentAndRules(COMPONENT_NAME, List.of(
                createRuleEvaluation(RuleActivationState.ACTIVE, Map.of("exempt-api-url-not-containing0", "ingress", "exempt-api-url-not-containing1", "internal-csp"))
        ));

        // URL DOES contain "internal-csp" -> not exempt
        SystemComponentHttpApiIgnoreFilter.Result result = filter.shouldIgnoreApi(createApi(COMPONENT_NAME, GovernanceServiceEnvironment.REF, "https://internal-csp.test-ref.foo.bar"));

        assertThat(result.ignore()).isFalse();
    }

    @Test
    void shouldIgnoreApi_urlContaining_ignored() {
        setupComponentAndRules(COMPONENT_NAME, List.of(
                createRuleEvaluation(RuleActivationState.ACTIVE, Map.of("exempt-api-url-containing0", "egress", "exempt-api-url-containing1", "-abn"))
        ));

        // URL DOES contain "egress" -> exempt
        SystemComponentHttpApiIgnoreFilter.Result result = filter.shouldIgnoreApi(createApi(COMPONENT_NAME, GovernanceServiceEnvironment.REF, "https://test-ref.egress.foo.bar"));

        assertThat(result.ignore()).isTrue();
        assertThat(result.reason()).contains("exempt-api-url-containing");
    }

    @Test
    void shouldIgnoreApi_urlNotContainingAnyExemptValue_notIgnored() {
        setupComponentAndRules(COMPONENT_NAME, List.of(
                createRuleEvaluation(RuleActivationState.ACTIVE, Map.of("exempt-api-url-containing0", "egress", "exempt-api-url-containing1", "-abn"))
        ));

        // URL does NOT contain any of the values -> not exempt
        SystemComponentHttpApiIgnoreFilter.Result result = filter.shouldIgnoreApi(createApi(COMPONENT_NAME, GovernanceServiceEnvironment.REF, "https://test-ref.ingress.foo.bar"));

        assertThat(result.ignore()).isFalse();
    }

    @Test
    void shouldIgnoreApi_exemptComponentName_exactMatch_ignored() {
        setupComponentAndRules(COMPONENT_NAME, List.of(
                createRuleEvaluation(RuleActivationState.ACTIVE, Map.of("exempt-component-names0", "test-service", "exempt-component-names1", "other-service"))
        ));

        SystemComponentHttpApiIgnoreFilter.Result result = filter.shouldIgnoreApi(createApi(COMPONENT_NAME, GovernanceServiceEnvironment.REF, "https://test-ref.ingress.foo.bar"));

        assertThat(result.ignore()).isTrue();
        assertThat(result.reason()).contains("exempt-component-names");
    }

    @Test
    void shouldIgnoreApi_exemptComponentName_suffixWildcard_ignored() {
        setupComponentAndRules(COMPONENT_NAME, List.of(
                createRuleEvaluation(RuleActivationState.ACTIVE, Map.of("exempt-component-names0", "*-service"))
        ));

        SystemComponentHttpApiIgnoreFilter.Result result = filter.shouldIgnoreApi(createApi(COMPONENT_NAME, GovernanceServiceEnvironment.REF, "https://test-ref.ingress.foo.bar"));

        assertThat(result.ignore()).isTrue();
        assertThat(result.reason()).contains("exempt-component-names");
    }

    @Test
    void shouldIgnoreApi_exemptComponentName_noMatch_notIgnored() {
        setupComponentAndRules(COMPONENT_NAME, List.of(
                createRuleEvaluation(RuleActivationState.ACTIVE, Map.of("exempt-component-names0", "other-service"))
        ));

        SystemComponentHttpApiIgnoreFilter.Result result = filter.shouldIgnoreApi(createApi(COMPONENT_NAME, GovernanceServiceEnvironment.REF, "https://test-ref.ingress.foo.bar"));

        assertThat(result.ignore()).isFalse();
    }

    @Test
    void shouldIgnoreApi_noExemptionParams_notIgnored() {
        setupComponentAndRules(COMPONENT_NAME, List.of(
                createRuleEvaluation(RuleActivationState.ACTIVE, Map.of())
        ));

        SystemComponentHttpApiIgnoreFilter.Result result = filter.shouldIgnoreApi(createApi(COMPONENT_NAME, GovernanceServiceEnvironment.REF, "https://test-ref.ingress.foo.bar"));

        assertThat(result.ignore()).isFalse();
    }

    // --- shouldIgnoreEndpoint tests ---

    @Test
    void shouldIgnoreEndpoint_componentNotFound_notIgnored() {
        when(systemComponentRepository.findByName(COMPONENT_NAME)).thenReturn(Optional.empty());

        SystemComponentHttpApiIgnoreFilter.Result result = filter.shouldIgnoreEndpoint(COMPONENT_NAME, new HttpEndpoint("/api/test", "GET"), "prod");

        assertThat(result.ignore()).isFalse();
    }

    @Test
    void shouldIgnoreEndpoint_ruleNotActive_ignored() {
        setupComponentAndRules(COMPONENT_NAME, List.of(
                createRuleEvaluation(RuleActivationState.EXEMPTED, Map.of())
        ));

        SystemComponentHttpApiIgnoreFilter.Result result = filter.shouldIgnoreEndpoint(COMPONENT_NAME, new HttpEndpoint("/api/test", "GET"), "prod");

        assertThat(result.ignore()).isTrue();
        assertThat(result.reason()).contains("exempted");
        assertThat(result.reason()).contains(COMPONENT_NAME);
    }

    @Test
    void shouldIgnoreEndpoint_ruleExemptedUntil_ignored() {
        setupComponentAndRules(COMPONENT_NAME, List.of(
                createRuleEvaluation(RuleActivationState.EXEMPTED_UNTIL, Map.of())
        ));

        SystemComponentHttpApiIgnoreFilter.Result result = filter.shouldIgnoreEndpoint(COMPONENT_NAME, new HttpEndpoint("/api/test", "GET"), "prod");

        assertThat(result.ignore()).isTrue();
        assertThat(result.reason()).contains("exempted");
    }

    @Test
    void shouldIgnoreEndpoint_exemptMethod_ignored() {
        setupComponentAndRules(COMPONENT_NAME, List.of(
                createRuleEvaluation(RuleActivationState.ACTIVE, Map.of("exempt-methods0", "POST", "exempt-methods1", "PUT"))
        ));

        SystemComponentHttpApiIgnoreFilter.Result result = filter.shouldIgnoreEndpoint(COMPONENT_NAME, new HttpEndpoint("/api/test", "POST"), "prod");

        assertThat(result.ignore()).isTrue();
        assertThat(result.reason()).contains("exempt-methods");
    }

    @Test
    void shouldIgnoreEndpoint_nonExemptMethod_notIgnored() {
        setupComponentAndRules(COMPONENT_NAME, List.of(
                createRuleEvaluation(RuleActivationState.ACTIVE, Map.of("exempt-methods0", "POST", "exempt-methods1", "PUT"))
        ));

        SystemComponentHttpApiIgnoreFilter.Result result = filter.shouldIgnoreEndpoint(COMPONENT_NAME, new HttpEndpoint("/api/test", "GET"), "prod");

        assertThat(result.ignore()).isFalse();
    }

    @Test
    void shouldIgnoreEndpoint_exemptPathExact_ignored() {
        setupComponentAndRules(COMPONENT_NAME, List.of(
                createRuleEvaluation(RuleActivationState.ACTIVE, Map.of("exempt-paths0", "/public/health", "exempt-paths1", "/foo/bar"))
        ));

        SystemComponentHttpApiIgnoreFilter.Result result = filter.shouldIgnoreEndpoint(COMPONENT_NAME, new HttpEndpoint("/foo/bar", "GET"), "prod");

        assertThat(result.ignore()).isTrue();
        assertThat(result.reason()).contains("exempt-paths");
    }

    @Test
    void shouldIgnoreEndpoint_exemptPathWildcard_ignored() {
        setupComponentAndRules(COMPONENT_NAME, List.of(
                createRuleEvaluation(RuleActivationState.ACTIVE, Map.of("exempt-paths0", "/public/*"))
        ));

        SystemComponentHttpApiIgnoreFilter.Result result = filter.shouldIgnoreEndpoint(COMPONENT_NAME, new HttpEndpoint("/public/health", "GET"), "prod");

        assertThat(result.ignore()).isTrue();
        assertThat(result.reason()).contains("exempt-paths");
    }

    @Test
    void shouldIgnoreEndpoint_nonMatchingPath_notIgnored() {
        setupComponentAndRules(COMPONENT_NAME, List.of(
                createRuleEvaluation(RuleActivationState.ACTIVE, Map.of("exempt-paths0", "/public/*"))
        ));

        SystemComponentHttpApiIgnoreFilter.Result result = filter.shouldIgnoreEndpoint(COMPONENT_NAME, new HttpEndpoint("/api/test", "GET"), "prod");

        assertThat(result.ignore()).isFalse();
    }

    @Test
    void shouldIgnoreEndpoint_exemptEndpointMethodPath_ignored() {
        setupComponentAndRules(COMPONENT_NAME, List.of(
                createRuleEvaluation(RuleActivationState.ACTIVE, Map.of("exempt-endpoints0", "GET:/foo/bar", "exempt-endpoints1", "PUT:/bar/*"))
        ));

        SystemComponentHttpApiIgnoreFilter.Result result = filter.shouldIgnoreEndpoint(COMPONENT_NAME, new HttpEndpoint("/foo/bar", "GET"), "prod");

        assertThat(result.ignore()).isTrue();
        assertThat(result.reason()).contains("exempt-endpoints");
    }

    @Test
    void shouldIgnoreEndpoint_exemptEndpointWildcard_ignored() {
        setupComponentAndRules(COMPONENT_NAME, List.of(
                createRuleEvaluation(RuleActivationState.ACTIVE, Map.of("exempt-endpoints0", "PUT:/bar/*"))
        ));

        SystemComponentHttpApiIgnoreFilter.Result result = filter.shouldIgnoreEndpoint(COMPONENT_NAME, new HttpEndpoint("/bar/foo", "PUT"), "prod");

        assertThat(result.ignore()).isTrue();
        assertThat(result.reason()).contains("exempt-endpoints");
    }

    @Test
    void shouldIgnoreEndpoint_exemptEndpointWrongMethod_notIgnored() {
        setupComponentAndRules(COMPONENT_NAME, List.of(
                createRuleEvaluation(RuleActivationState.ACTIVE, Map.of("exempt-endpoints0", "GET:/foo/bar"))
        ));

        SystemComponentHttpApiIgnoreFilter.Result result = filter.shouldIgnoreEndpoint(COMPONENT_NAME, new HttpEndpoint("/foo/bar", "POST"), "prod");

        assertThat(result.ignore()).isFalse();
    }

    @Test
    void shouldIgnoreEndpoint_exemptComponentName_exactMatch_ignored() {
        setupComponentAndRules(COMPONENT_NAME, List.of(
                createRuleEvaluation(RuleActivationState.ACTIVE, Map.of("exempt-component-names0", "test-service"))
        ));

        SystemComponentHttpApiIgnoreFilter.Result result = filter.shouldIgnoreEndpoint(COMPONENT_NAME, new HttpEndpoint("/api/test", "GET"), "prod");

        assertThat(result.ignore()).isTrue();
        assertThat(result.reason()).contains("exempt-component-names");
    }

    @Test
    void shouldIgnoreEndpoint_exemptComponentName_prefixWildcard_ignored() {
        setupComponentAndRules(COMPONENT_NAME, List.of(
                createRuleEvaluation(RuleActivationState.ACTIVE, Map.of("exempt-component-names0", "test-*"))
        ));

        SystemComponentHttpApiIgnoreFilter.Result result = filter.shouldIgnoreEndpoint(COMPONENT_NAME, new HttpEndpoint("/api/test", "GET"), "prod");

        assertThat(result.ignore()).isTrue();
        assertThat(result.reason()).contains("exempt-component-names");
    }

    @Test
    void shouldIgnoreEndpoint_exemptComponentName_noMatch_notIgnored() {
        setupComponentAndRules(COMPONENT_NAME, List.of(
                createRuleEvaluation(RuleActivationState.ACTIVE, Map.of("exempt-component-names0", "other-*"))
        ));

        SystemComponentHttpApiIgnoreFilter.Result result = filter.shouldIgnoreEndpoint(COMPONENT_NAME, new HttpEndpoint("/api/test", "GET"), "prod");

        assertThat(result.ignore()).isFalse();
    }

    @Test
    void shouldIgnoreEndpoint_noExemptions_notIgnored() {
        setupComponentAndRules(COMPONENT_NAME, List.of(
                createRuleEvaluation(RuleActivationState.ACTIVE, Map.of())
        ));

        SystemComponentHttpApiIgnoreFilter.Result result = filter.shouldIgnoreEndpoint(COMPONENT_NAME, new HttpEndpoint("/api/test", "GET"), "prod");

        assertThat(result.ignore()).isFalse();
    }

    @Test
    void shouldIgnoreEndpoint_invalidExemptEndpointsAtRuntime_throws() {
        setupComponentAndRules(COMPONENT_NAME, List.of(
                createRuleEvaluation(RuleActivationState.ACTIVE, Map.of("exempt-endpoints0", "/missing-method"))
        ));
        HttpEndpoint endpoint = new HttpEndpoint("/api/test", "GET");

        assertThatThrownBy(() -> filter.shouldIgnoreEndpoint(COMPONENT_NAME, endpoint, "prod"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("/missing-method");
    }

    @Test
    void shouldIgnoreApi_invalidExemptEndpointsAtRuntime_throws() {
        setupComponentAndRules(COMPONENT_NAME, List.of(
                createRuleEvaluation(RuleActivationState.ACTIVE, Map.of("exempt-endpoints0", "/missing-method"))
        ));
        SystemComponentHttpApi api = createApi(COMPONENT_NAME, GovernanceServiceEnvironment.REF, "https://test-ref.ingress.foo.bar");

        assertThatThrownBy(() -> filter.shouldIgnoreApi(api))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("/missing-method");
    }

    // --- Helper methods ---

    private void setupComponentAndRules(String componentName, List<RuleEvaluation> ruleEvaluations) {
        SystemComponent component = mock(SystemComponent.class);
        when(systemComponentRepository.findByName(componentName)).thenReturn(Optional.of(component));
        when(ruleRepository.getRulesToEvaluateForComponent(component)).thenReturn(ruleEvaluations);
    }

    private RuleEvaluation createRuleEvaluation(RuleActivationState activationState, Map<String, String> parameters) {
        Rule rule = mock(Rule.class);
        RuleMetadata metadata = RuleMetadata.builder()
                .ruleId(RuleId.of("endpoints-protected"))
                .label("REST Endpoint Security (Scanner)")
                .build();
        when(rule.metadata()).thenReturn(metadata);
        return new RuleEvaluation(rule, new RuleParameters(parameters), activationState);
    }

    private SystemComponentHttpApi createApi(String componentName, GovernanceServiceEnvironment environment, String url) {
        HttpApi httpApi = new HttpApi(url, "1.0", List.of(new HttpEndpoint("/api/test", "GET")));
        return new SystemComponentHttpApi(componentName, environment, httpApi, null);
    }
}
