package ch.admin.bit.jeap.governance.domain.plugin.security.api;

import ch.admin.bit.jeap.governance.domain.GovernanceServiceEnvironment;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleParameters;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpApiExemptionsTest {

    // --- Constructor validation tests ---

    @Test
    void constructor_validExemptEndpoints_noException() {
        assertThatNoException().isThrownBy(() ->
                new HttpApiExemptions(new RuleParameters(Map.of("exempt-endpoints0", "GET:/api/*", "exempt-endpoints1", "DELETE:/admin/users"))));
    }

    @Test
    void constructor_noExemptEndpoints_noException() {
        assertThatNoException().isThrownBy(() ->
                new HttpApiExemptions(new RuleParameters(Map.of())));
    }

    @Test
    void constructor_invalidExemptEndpoints_throws() {
        RuleParameters params = new RuleParameters(Map.of("exempt-endpoints0", "/api/resource"));
        assertThatThrownBy(() -> new HttpApiExemptions(params))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("/api/resource")
                .hasMessageContaining("METHOD:PATH");
    }

    @Test
    void constructor_emptyMethod_throws() {
        RuleParameters params = new RuleParameters(Map.of("exempt-endpoints0", ":/api/resource"));
        assertThatThrownBy(() -> new HttpApiExemptions(params))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(":/api/resource");
    }

    @Test
    void constructor_mixedValidAndInvalid_throws() {
        RuleParameters params = new RuleParameters(Map.of("exempt-endpoints0", "GET:/api/*", "exempt-endpoints1", "/bad/path"));
        assertThatThrownBy(() -> new HttpApiExemptions(params))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("/bad/path");
    }

    // --- validateParameters tests ---

    @Test
    void validateParameters_validFormat_noException() {
        assertThatNoException().isThrownBy(() ->
                HttpApiExemptions.validateParameters(new RuleParameters(Map.of("exempt-endpoints0", "GET:/api/*"))));
    }

    @Test
    void validateParameters_nullExemptEndpoints_noException() {
        assertThatNoException().isThrownBy(() ->
                HttpApiExemptions.validateParameters(new RuleParameters(Map.of())));
    }

    @Test
    void validateParameters_invalidFormat_throws() {
        RuleParameters params = new RuleParameters(Map.of("exempt-endpoints0", "/bad"));
        assertThatThrownBy(() -> HttpApiExemptions.validateParameters(params))
                .isInstanceOf(IllegalStateException.class);
    }

    // --- shouldExemptComponent tests ---

    @Test
    void shouldExemptComponent_exactMatch() {
        HttpApiExemptions exemptions = new HttpApiExemptions(new RuleParameters(Map.of("exempt-component-names0", "test-service", "exempt-component-names1", "other-service")));

        HttpApiExemptions.Result result = exemptions.shouldExemptComponent("test-service");

        assertThat(result.exempted()).isTrue();
        assertThat(result.reason()).contains("exempt-component-names");
    }

    @Test
    void shouldExemptComponent_prefixWildcard() {
        HttpApiExemptions exemptions = new HttpApiExemptions(new RuleParameters(Map.of("exempt-component-names0", "test-*")));

        assertThat(exemptions.shouldExemptComponent("test-service").exempted()).isTrue();
        assertThat(exemptions.shouldExemptComponent("test-other").exempted()).isTrue();
        assertThat(exemptions.shouldExemptComponent("prod-service").exempted()).isFalse();
    }

    @Test
    void shouldExemptComponent_suffixWildcard() {
        HttpApiExemptions exemptions = new HttpApiExemptions(new RuleParameters(Map.of("exempt-component-names0", "*-scs")));

        assertThat(exemptions.shouldExemptComponent("my-app-scs").exempted()).isTrue();
        assertThat(exemptions.shouldExemptComponent("other-scs").exempted()).isTrue();
        assertThat(exemptions.shouldExemptComponent("my-service").exempted()).isFalse();
    }

    @Test
    void shouldExemptComponent_noMatch() {
        HttpApiExemptions exemptions = new HttpApiExemptions(new RuleParameters(Map.of("exempt-component-names0", "other-service")));

        HttpApiExemptions.Result result = exemptions.shouldExemptComponent("test-service");

        assertThat(result.exempted()).isFalse();
    }

    @Test
    void shouldExemptComponent_noParams_notExempted() {
        HttpApiExemptions exemptions = new HttpApiExemptions(new RuleParameters(Map.of()));

        HttpApiExemptions.Result result = exemptions.shouldExemptComponent("test-service");

        assertThat(result.exempted()).isFalse();
    }

    // --- shouldExemptHttpEndpoint tests ---

    @Test
    void shouldExemptHttpEndpoint_exemptMethods_match() {
        HttpApiExemptions exemptions = new HttpApiExemptions(new RuleParameters(Map.of("exempt-methods0", "POST", "exempt-methods1", "PUT")));

        HttpApiExemptions.Result result = exemptions.shouldExemptHttpEndpoint(new HttpEndpoint("/api/test", "POST"), "prod");

        assertThat(result.exempted()).isTrue();
        assertThat(result.reason()).contains("exempt-methods");
    }

    @Test
    void shouldExemptHttpEndpoint_exemptMethods_noMatch() {
        HttpApiExemptions exemptions = new HttpApiExemptions(new RuleParameters(Map.of("exempt-methods0", "POST", "exempt-methods1", "PUT")));

        HttpApiExemptions.Result result = exemptions.shouldExemptHttpEndpoint(new HttpEndpoint("/api/test", "GET"), "prod");

        assertThat(result.exempted()).isFalse();
    }

    @Test
    void shouldExemptHttpEndpoint_exemptMethods_caseInsensitive() {
        HttpApiExemptions exemptions = new HttpApiExemptions(new RuleParameters(Map.of("exempt-methods0", "POST")));

        HttpApiExemptions.Result result = exemptions.shouldExemptHttpEndpoint(new HttpEndpoint("/api/test", "post"), "prod");

        assertThat(result.exempted()).isTrue();
    }

    @Test
    void shouldExemptHttpEndpoint_exemptPaths_exactMatch() {
        HttpApiExemptions exemptions = new HttpApiExemptions(new RuleParameters(Map.of("exempt-paths0", "/public/health", "exempt-paths1", "/foo/bar")));

        HttpApiExemptions.Result result = exemptions.shouldExemptHttpEndpoint(new HttpEndpoint("/foo/bar", "GET"), "prod");

        assertThat(result.exempted()).isTrue();
        assertThat(result.reason()).contains("exempt-paths");
    }

    @Test
    void shouldExemptHttpEndpoint_exemptPaths_wildcardMatch() {
        HttpApiExemptions exemptions = new HttpApiExemptions(new RuleParameters(Map.of("exempt-paths0", "/public/*")));

        HttpApiExemptions.Result result = exemptions.shouldExemptHttpEndpoint(new HttpEndpoint("/public/health", "GET"), "prod");

        assertThat(result.exempted()).isTrue();
        assertThat(result.reason()).contains("exempt-paths");
    }

    @Test
    void shouldExemptHttpEndpoint_exemptPaths_suffixWildcardMatch() {
        HttpApiExemptions exemptions = new HttpApiExemptions(new RuleParameters(Map.of("exempt-paths0", "*/health")));

        assertThat(exemptions.shouldExemptHttpEndpoint(new HttpEndpoint("/public/health", "GET"), "prod").exempted()).isTrue();
        assertThat(exemptions.shouldExemptHttpEndpoint(new HttpEndpoint("/api/health", "GET"), "prod").exempted()).isTrue();
        assertThat(exemptions.shouldExemptHttpEndpoint(new HttpEndpoint("/api/test", "GET"), "prod").exempted()).isFalse();
    }

    @Test
    void shouldExemptHttpEndpoint_exemptPaths_noMatch() {
        HttpApiExemptions exemptions = new HttpApiExemptions(new RuleParameters(Map.of("exempt-paths0", "/public/*")));

        HttpApiExemptions.Result result = exemptions.shouldExemptHttpEndpoint(new HttpEndpoint("/api/test", "GET"), "prod");

        assertThat(result.exempted()).isFalse();
    }

    @Test
    void shouldExemptHttpEndpoint_exemptEndpoints_methodPathMatch() {
        HttpApiExemptions exemptions = new HttpApiExemptions(new RuleParameters(Map.of("exempt-endpoints0", "GET:/foo/bar", "exempt-endpoints1", "PUT:/bar/*")));

        HttpApiExemptions.Result result = exemptions.shouldExemptHttpEndpoint(new HttpEndpoint("/foo/bar", "GET"), "prod");

        assertThat(result.exempted()).isTrue();
        assertThat(result.reason()).contains("exempt-endpoints");
    }

    @Test
    void shouldExemptHttpEndpoint_exemptEndpoints_wildcardMatch() {
        HttpApiExemptions exemptions = new HttpApiExemptions(new RuleParameters(Map.of("exempt-endpoints0", "PUT:/bar/*")));

        HttpApiExemptions.Result result = exemptions.shouldExemptHttpEndpoint(new HttpEndpoint("/bar/foo", "PUT"), "prod");

        assertThat(result.exempted()).isTrue();
    }

    @Test
    void shouldExemptHttpEndpoint_exemptEndpoints_suffixWildcardMatch() {
        HttpApiExemptions exemptions = new HttpApiExemptions(new RuleParameters(Map.of("exempt-endpoints0", "GET:*/health")));

        assertThat(exemptions.shouldExemptHttpEndpoint(new HttpEndpoint("/public/health", "GET"), "prod").exempted()).isTrue();
        assertThat(exemptions.shouldExemptHttpEndpoint(new HttpEndpoint("/api/health", "GET"), "prod").exempted()).isTrue();
        assertThat(exemptions.shouldExemptHttpEndpoint(new HttpEndpoint("/api/test", "GET"), "prod").exempted()).isFalse();
    }

    @Test
    void shouldExemptHttpEndpoint_exemptEndpoints_methodMismatch() {
        HttpApiExemptions exemptions = new HttpApiExemptions(new RuleParameters(Map.of("exempt-endpoints0", "GET:/foo/bar")));

        HttpApiExemptions.Result result = exemptions.shouldExemptHttpEndpoint(new HttpEndpoint("/foo/bar", "POST"), "prod");

        assertThat(result.exempted()).isFalse();
    }

    @Test
    void shouldExemptHttpEndpoint_exemptEndpoints_caseInsensitiveMethod() {
        HttpApiExemptions exemptions = new HttpApiExemptions(new RuleParameters(Map.of("exempt-endpoints0", "GET:/foo/bar")));

        HttpApiExemptions.Result result = exemptions.shouldExemptHttpEndpoint(new HttpEndpoint("/foo/bar", "get"), "prod");

        assertThat(result.exempted()).isTrue();
    }

    @Test
    void shouldExemptHttpEndpoint_combinedParams_firstMatchWins() {
        Map<String, String> params = new HashMap<>();
        params.put("exempt-methods0", "POST");
        params.put("exempt-paths0", "/foo/*");
        params.put("exempt-endpoints0", "GET:/bar");
        HttpApiExemptions exemptions = new HttpApiExemptions(new RuleParameters(params));

        // Method match comes first
        HttpApiExemptions.Result result = exemptions.shouldExemptHttpEndpoint(new HttpEndpoint("/foo/bar", "POST"), "prod");
        assertThat(result.exempted()).isTrue();
        assertThat(result.reason()).contains("exempt-methods");
    }

    @Test
    void shouldExemptHttpEndpoint_noParams_notExempted() {
        HttpApiExemptions exemptions = new HttpApiExemptions(new RuleParameters(Map.of()));

        HttpApiExemptions.Result result = exemptions.shouldExemptHttpEndpoint(new HttpEndpoint("/api/test", "GET"), "prod");

        assertThat(result.exempted()).isFalse();
    }

    // --- Default exemption tests ---

    static Stream<Arguments> defaultExemptionCases() {
        return Stream.of(
                Arguments.of("/actuator/health", "default-exemption: actuator"),
                Arguments.of("/my-scs/actuator", "default-exemption: actuator"),
                Arguments.of("/api/v3/configuration", "default-exemption: api-configuration")
        );
    }

    @ParameterizedTest
    @MethodSource("defaultExemptionCases")
    void shouldExemptHttpEndpoint_defaultExemption(String path, String expectedReason) {
        HttpApiExemptions exemptions = new HttpApiExemptions(new RuleParameters(Map.of()));

        HttpApiExemptions.Result result = exemptions.shouldExemptHttpEndpoint(new HttpEndpoint(path, "GET"), "prod");

        assertThat(result.exempted()).isTrue();
        assertThat(result.reason()).isEqualTo(expectedReason);
    }

    @Test
    void shouldExemptHttpEndpoint_defaultExemption_uiProviderWildcard() {
        HttpApiExemptions exemptions = new HttpApiExemptions(new RuleParameters(Map.of()));

        assertThat(exemptions.shouldExemptHttpEndpoint(new HttpEndpoint("/**", "GET"), "prod").exempted()).isTrue();
        assertThat(exemptions.shouldExemptHttpEndpoint(new HttpEndpoint("/**", "GET"), "prod").reason()).isEqualTo("default-exemption: ui-provider");
    }

    @Test
    void shouldExemptHttpEndpoint_defaultExemption_uiProviderRoot() {
        HttpApiExemptions exemptions = new HttpApiExemptions(new RuleParameters(Map.of()));

        assertThat(exemptions.shouldExemptHttpEndpoint(new HttpEndpoint("/", "GET"), "prod").exempted()).isTrue();
        assertThat(exemptions.shouldExemptHttpEndpoint(new HttpEndpoint("/", "GET"), "prod").reason()).isEqualTo("default-exemption: ui-provider");
    }

    @Test
    void shouldExemptHttpEndpoint_defaultExemption_apiDocsOnRef() {
        HttpApiExemptions exemptions = new HttpApiExemptions(new RuleParameters(Map.of()));

        assertThat(exemptions.shouldExemptHttpEndpoint(new HttpEndpoint("/api-docs", "GET"), "ref").exempted()).isTrue();
        assertThat(exemptions.shouldExemptHttpEndpoint(new HttpEndpoint("/api-docs", "GET"), "ref").reason()).isEqualTo("default-exemption: api-docs-swagger-ref");
    }

    @Test
    void shouldExemptHttpEndpoint_defaultExemption_swaggerOnRef() {
        HttpApiExemptions exemptions = new HttpApiExemptions(new RuleParameters(Map.of()));

        assertThat(exemptions.shouldExemptHttpEndpoint(new HttpEndpoint("/swagger-ui", "GET"), "ref").exempted()).isTrue();
        assertThat(exemptions.shouldExemptHttpEndpoint(new HttpEndpoint("/swagger-ui", "GET"), "ref").reason()).isEqualTo("default-exemption: api-docs-swagger-ref");
    }

    @Test
    void shouldExemptHttpEndpoint_defaultExemption_apiDocsNotOnNonRef() {
        HttpApiExemptions exemptions = new HttpApiExemptions(new RuleParameters(Map.of()));

        assertThat(exemptions.shouldExemptHttpEndpoint(new HttpEndpoint("/api-docs", "GET"), "prod").exempted()).isFalse();
        assertThat(exemptions.shouldExemptHttpEndpoint(new HttpEndpoint("/swagger-ui", "GET"), "abn").exempted()).isFalse();
    }

    @Test
    void shouldExemptHttpEndpoint_defaultExemption_regularPathNotExempted() {
        HttpApiExemptions exemptions = new HttpApiExemptions(new RuleParameters(Map.of()));

        HttpApiExemptions.Result result = exemptions.shouldExemptHttpEndpoint(new HttpEndpoint("/api/users", "GET"), "prod");

        assertThat(result.exempted()).isFalse();
    }

    @Test
    void shouldExemptHttpEndpoint_disableDefaultExemptions_actuatorNotExempted() {
        HttpApiExemptions exemptions = new HttpApiExemptions(new RuleParameters(Map.of("disable-default-exemptions", "true")));

        HttpApiExemptions.Result result = exemptions.shouldExemptHttpEndpoint(new HttpEndpoint("/actuator/health", "GET"), "prod");

        assertThat(result.exempted()).isFalse();
    }

    @Test
    void shouldExemptHttpEndpoint_disableDefaultExemptions_parameterExemptionsStillWork() {
        HttpApiExemptions exemptions = new HttpApiExemptions(new RuleParameters(Map.of(
                "disable-default-exemptions", "true",
                "exempt-paths0", "/public/*"
        )));

        assertThat(exemptions.shouldExemptHttpEndpoint(new HttpEndpoint("/actuator/health", "GET"), "prod").exempted()).isFalse();
        assertThat(exemptions.shouldExemptHttpEndpoint(new HttpEndpoint("/public/info", "GET"), "prod").exempted()).isTrue();
    }

    // --- isApiConfigurationCall tests ---

    @ParameterizedTest
    @CsvSource({
            "/api/configuration, true",
            "/ui/configuration, true",
            "/ui/scs-configuration, true",
            "/ui-api/app-configuration, true",
            "/ui-api/configuration, true",
            "/ui-api/configurations, true",
            "/api/v1/configuration, true",
            "/api/v2/configuration, true",
            "/api/configuration/, true",
            "/api/configuration/sub-configuration, true",
            "/api/configuration/sub-configuration/foo/bar, true",
            "/api/my-config, false",
            "/api/v1/declaration, false",
            "/api/config, false",
            "/api/foobar/, false"
    })
    void isApiConfigurationCall(String path, boolean expected) {
        assertThat(HttpApiExemptions.isApiConfigurationCall(path)).isEqualTo(expected);
    }

    // --- shouldExemptHttpApi tests ---

    @Test
    void shouldExemptHttpApi_exemptEnvironments_match() {
        HttpApiExemptions exemptions = new HttpApiExemptions(new RuleParameters(Map.of("exempt-environments0", "ABN", "exempt-environments1", "PROD")));

        HttpApiExemptions.Result result = exemptions.shouldExemptHttpApi(createApi("test-service", GovernanceServiceEnvironment.ABN, "https://test.ingress.foo"));

        assertThat(result.exempted()).isTrue();
        assertThat(result.reason()).contains("exempt-environments");
    }

    @Test
    void shouldExemptHttpApi_exemptEnvironments_noMatch() {
        HttpApiExemptions exemptions = new HttpApiExemptions(new RuleParameters(Map.of("exempt-environments0", "ABN", "exempt-environments1", "PROD")));

        HttpApiExemptions.Result result = exemptions.shouldExemptHttpApi(createApi("test-service", GovernanceServiceEnvironment.REF, "https://test.ingress.foo"));

        assertThat(result.exempted()).isFalse();
    }

    @Test
    void shouldExemptHttpApi_exemptEnvironments_caseInsensitive() {
        HttpApiExemptions exemptions = new HttpApiExemptions(new RuleParameters(Map.of("exempt-environments0", "abn")));

        HttpApiExemptions.Result result = exemptions.shouldExemptHttpApi(createApi("test-service", GovernanceServiceEnvironment.ABN, "https://test.foo"));

        assertThat(result.exempted()).isTrue();
    }

    @Test
    void shouldExemptHttpApi_urlNotContaining_urlLacksAllStrings_exempted() {
        HttpApiExemptions exemptions = new HttpApiExemptions(new RuleParameters(Map.of("exempt-api-url-not-containing0", "ingress", "exempt-api-url-not-containing1", "internal-csp")));

        // URL does NOT contain "ingress" or "internal-csp" -> exempt
        HttpApiExemptions.Result result = exemptions.shouldExemptHttpApi(createApi("test-service", GovernanceServiceEnvironment.REF, "https://test-ref.foo.bar"));

        assertThat(result.exempted()).isTrue();
        assertThat(result.reason()).contains("exempt-api-url-not-containing");
    }

    @Test
    void shouldExemptHttpApi_urlNotContaining_urlContainsOneString_notExempted() {
        HttpApiExemptions exemptions = new HttpApiExemptions(new RuleParameters(Map.of("exempt-api-url-not-containing0", "ingress", "exempt-api-url-not-containing1", "internal-csp")));

        // URL DOES contain "ingress" -> not exempt
        HttpApiExemptions.Result result = exemptions.shouldExemptHttpApi(createApi("test-service", GovernanceServiceEnvironment.REF, "https://test-ref.ingress.foo.bar"));

        assertThat(result.exempted()).isFalse();
    }

    @Test
    void shouldExemptHttpApi_urlContaining_urlContainsString_exempted() {
        HttpApiExemptions exemptions = new HttpApiExemptions(new RuleParameters(Map.of("exempt-api-url-containing0", "egress", "exempt-api-url-containing1", "-abn")));

        // URL DOES contain "egress" -> exempt
        HttpApiExemptions.Result result = exemptions.shouldExemptHttpApi(createApi("test-service", GovernanceServiceEnvironment.REF, "https://test-ref.egress.foo.bar"));

        assertThat(result.exempted()).isTrue();
        assertThat(result.reason()).contains("exempt-api-url-containing");
    }

    @Test
    void shouldExemptHttpApi_urlContaining_urlLacksAll_notExempted() {
        HttpApiExemptions exemptions = new HttpApiExemptions(new RuleParameters(Map.of("exempt-api-url-containing0", "egress", "exempt-api-url-containing1", "-abn")));

        // URL does NOT contain any of the values -> not exempt
        HttpApiExemptions.Result result = exemptions.shouldExemptHttpApi(createApi("test-service", GovernanceServiceEnvironment.REF, "https://test-ref.ingress.foo.bar"));

        assertThat(result.exempted()).isFalse();
    }

    @Test
    void shouldExemptHttpApi_nullHttpApiUrl_notExempted() {
        HttpApiExemptions exemptions = new HttpApiExemptions(new RuleParameters(Map.of("exempt-api-url-not-containing0", "ingress")));

        SystemComponentHttpApi api = new SystemComponentHttpApi("test-service", GovernanceServiceEnvironment.REF, null, null);
        HttpApiExemptions.Result result = exemptions.shouldExemptHttpApi(api);

        assertThat(result.exempted()).isFalse();
    }

    @Test
    void shouldExemptHttpApi_noParams_notExempted() {
        HttpApiExemptions exemptions = new HttpApiExemptions(new RuleParameters(Map.of()));

        HttpApiExemptions.Result result = exemptions.shouldExemptHttpApi(createApi("test-service", GovernanceServiceEnvironment.REF, "https://test.ingress.foo"));

        assertThat(result.exempted()).isFalse();
    }

    // --- Helper method tests ---

    static Stream<Arguments> matchesPatternCases() {
        return Stream.of(
                // Exact match
                Arguments.of("/foo/bar", "/foo/bar", true),
                Arguments.of("/foo/bar", "/foo", false),
                // Prefix wildcard (startsWith)
                Arguments.of("/foo/bar", "/foo*", true),
                Arguments.of("/foo/bar", "/foo/bar*", true),
                Arguments.of("/foo/bar", "/bar*", false),
                Arguments.of("/foo/bar", "/foo/dummy*", false),
                // Suffix wildcard (endsWith)
                Arguments.of("/foo/bar", "*/bar", true),
                Arguments.of("/foo/bar", "*bar", true),
                Arguments.of("/foo/bar", "*/foo", false),
                Arguments.of("/foo/bar", "*baz", false),
                Arguments.of("my-service", "*-service", true),
                Arguments.of("my-scs", "*-service", false)
        );
    }

    @ParameterizedTest
    @MethodSource("matchesPatternCases")
    void matchesPattern(String value, String pattern, boolean expected) {
        assertThat(HttpApiExemptions.matchesPattern(value, pattern)).isEqualTo(expected);
    }

    // --- Helper methods ---

    private static SystemComponentHttpApi createApi(String componentName, GovernanceServiceEnvironment environment, String url) {
        HttpApi httpApi = new HttpApi(url, "1.0", List.of(new HttpEndpoint("/api/test", "GET")));
        return new SystemComponentHttpApi(componentName, environment, httpApi, null);
    }
}
