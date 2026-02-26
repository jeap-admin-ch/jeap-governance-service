package ch.admin.bit.jeap.governance.domain.plugin.security.api;

import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleParameters;

import java.util.List;

public class HttpApiExemptions {

    public static final String EXEMPT_METHODS_KEY = "exempt-methods";
    public static final String EXEMPT_PATHS_KEY = "exempt-paths";
    public static final String EXEMPT_ENDPOINTS_KEY = "exempt-endpoints";
    public static final String EXEMPT_ENVIRONMENTS_KEY = "exempt-environments";
    public static final String EXEMPT_API_URL_NOT_CONTAINING_KEY = "exempt-api-url-not-containing";
    public static final String EXEMPT_COMPONENT_NAMES_KEY = "exempt-component-names";
    public static final String EXEMPT_API_URL_CONTAINING_KEY = "exempt-api-url-containing";
    public static final String DISABLE_DEFAULT_EXEMPTIONS_KEY = "disable-default-exemptions";

    private final RuleParameters ruleParameters;

    public HttpApiExemptions(RuleParameters ruleParameters) {
        this.ruleParameters = ruleParameters;
        validateExemptEndpointsFormat(ruleParameters.getParameterAsList(EXEMPT_ENDPOINTS_KEY));
    }

    public Result shouldExemptHttpApi(SystemComponentHttpApi api) {
        // Check exempt-environments
        List<String> exemptEnvironments = ruleParameters.getParameterAsList(EXEMPT_ENVIRONMENTS_KEY);
        if (!exemptEnvironments.isEmpty() && exemptEnvironments.stream()
                .anyMatch(env -> env.equalsIgnoreCase(api.environment().name()))) {
            return Result.exemptedWithReason(EXEMPT_ENVIRONMENTS_KEY + ": " + String.join(", ", exemptEnvironments));
        }

        String apiUrl = api.httpApi() != null ? api.httpApi().url() : null;
        if (apiUrl != null) {
            // Check exempt-api-url-not-containing: exempt if URL does NOT contain ANY of the specified strings
            List<String> notContainingValues = ruleParameters.getParameterAsList(EXEMPT_API_URL_NOT_CONTAINING_KEY);
            if (!notContainingValues.isEmpty() && notContainingValues.stream().noneMatch(apiUrl::contains)) {
                return Result.exemptedWithReason(EXEMPT_API_URL_NOT_CONTAINING_KEY + ": " + String.join(", ", notContainingValues));
            }

            // Check exempt-api-url-containing: exempt if URL DOES contain any of the specified strings
            List<String> containingValues = ruleParameters.getParameterAsList(EXEMPT_API_URL_CONTAINING_KEY);
            if (!containingValues.isEmpty() && containingValues.stream().anyMatch(apiUrl::contains)) {
                return Result.exemptedWithReason(EXEMPT_API_URL_CONTAINING_KEY + ": " + String.join(", ", containingValues));
            }
        }

        return Result.notExempted();
    }

    public Result shouldExemptComponent(String systemComponentName) {
        List<String> exemptComponentNames = ruleParameters.getParameterAsList(EXEMPT_COMPONENT_NAMES_KEY);
        if (!exemptComponentNames.isEmpty() && matchesAnyPattern(systemComponentName, exemptComponentNames)) {
            return Result.exemptedWithReason(EXEMPT_COMPONENT_NAMES_KEY + ": " + String.join(", ", exemptComponentNames));
        }
        return Result.notExempted();
    }

    public Result shouldExemptHttpEndpoint(HttpEndpoint httpEndpoint, String environment) {
        Result result = checkDefaultExemptions(httpEndpoint, environment);
        if (result.exempted()) {
            return result;
        }

        result = checkExemptMethods(httpEndpoint);
        if (result.exempted()) {
            return result;
        }

        result = checkExemptPaths(httpEndpoint);
        if (result.exempted()) {
            return result;
        }

        return checkExemptEndpoints(httpEndpoint);
    }

    private Result checkDefaultExemptions(HttpEndpoint httpEndpoint, String environment) {
        if ("true".equalsIgnoreCase(ruleParameters.parameters().get(DISABLE_DEFAULT_EXEMPTIONS_KEY))) {
            return Result.notExempted();
        }

        String path = httpEndpoint.path();

        if (path.contains("/actuator")) {
            return Result.exemptedWithReason("default-exemption: actuator");
        }
        if (path.equals("/**") || path.equals("/")) {
            return Result.exemptedWithReason("default-exemption: ui-provider");
        }
        if (isApiConfigurationCall(path)) {
            return Result.exemptedWithReason("default-exemption: api-configuration");
        }
        if ("ref".equalsIgnoreCase(environment) && (path.startsWith("/api-docs") || path.startsWith("/swagger-ui"))) {
            return Result.exemptedWithReason("default-exemption: api-docs-swagger-ref");
        }

        return Result.notExempted();
    }

    private Result checkExemptMethods(HttpEndpoint httpEndpoint) {
        List<String> exemptMethods = ruleParameters.getParameterAsList(EXEMPT_METHODS_KEY);
        if (!exemptMethods.isEmpty() && exemptMethods.stream()
                .anyMatch(method -> method.equalsIgnoreCase(httpEndpoint.method()))) {
            return Result.exemptedWithReason(EXEMPT_METHODS_KEY + ": " + String.join(", ", exemptMethods));
        }
        return Result.notExempted();
    }

    private Result checkExemptPaths(HttpEndpoint httpEndpoint) {
        List<String> exemptPaths = ruleParameters.getParameterAsList(EXEMPT_PATHS_KEY);
        if (!exemptPaths.isEmpty() && matchesAnyPattern(httpEndpoint.path(), exemptPaths)) {
            return Result.exemptedWithReason(EXEMPT_PATHS_KEY + ": " + String.join(", ", exemptPaths));
        }
        return Result.notExempted();
    }

    private Result checkExemptEndpoints(HttpEndpoint httpEndpoint) {
        List<String> exemptEndpoints = ruleParameters.getParameterAsList(EXEMPT_ENDPOINTS_KEY);
        for (String exemptEndpoint : exemptEndpoints) {
            String[] parts = exemptEndpoint.split(":", 2);
            String exemptMethod = parts[0].trim();
            String exemptPath = parts[1].trim();
            if (exemptMethod.equalsIgnoreCase(httpEndpoint.method()) && matchesPattern(httpEndpoint.path(), exemptPath)) {
                return Result.exemptedWithReason(EXEMPT_ENDPOINTS_KEY + ": " + String.join(", ", exemptEndpoints));
            }
        }
        return Result.notExempted();
    }

    static boolean isApiConfigurationCall(String path) {
        return path.matches("/(ui-)?(ui)?(api)?(/)?(\\w)*/((\\w)*-)?configuration(\\S)*");
    }

    public static void validateParameters(RuleParameters ruleParameters) {
        validateExemptEndpointsFormat(ruleParameters.getParameterAsList(EXEMPT_ENDPOINTS_KEY));
    }

    static boolean matchesAnyPattern(String value, List<String> patterns) {
        return patterns.stream().anyMatch(pattern -> matchesPattern(value, pattern));
    }

    /**
     * Matches a value against a simple pattern. Supported pattern types:
     * <ul>
     *   <li>Exact match: {@code "/foo/bar"} matches only {@code "/foo/bar"}</li>
     *   <li>Prefix wildcard (trailing {@code *}): {@code "/api/*"} matches any value starting with {@code "/api/"}</li>
     *   <li>Suffix wildcard (leading {@code *}): {@code "*-service"} matches any value ending with {@code "-service"}</li>
     * </ul>
     * Middle wildcards (e.g., {@code "/api/&#42;/users"}) and multiple wildcards are not supported.
     */
    static boolean matchesPattern(String value, String pattern) {
        if (pattern.startsWith("*")) {
            return value.endsWith(pattern.substring(1));
        } else if (pattern.endsWith("*")) {
            return value.startsWith(pattern.substring(0, pattern.length() - 1));
        } else {
            return value.equals(pattern);
        }
    }

    private static void validateExemptEndpointsFormat(List<String> entries) {
        for (String entry : entries) {
            String[] parts = entry.split(":", 2);
            if (parts.length != 2 || parts[0].trim().isEmpty() || parts[1].trim().isEmpty()) {
                throw new IllegalStateException(
                        "Invalid exempt-endpoints entry '%s'. Expected format 'METHOD:PATH' (e.g. 'GET:/api/*').".formatted(entry));
            }
        }
    }

    public record Result(boolean exempted, String reason) {
        private static final Result NOT_EXEMPTED = new Result(false, null);

        public static Result exemptedWithReason(String reason) {
            return new Result(true, reason);
        }

        public static Result notExempted() {
            return NOT_EXEMPTED;
        }
    }
}
