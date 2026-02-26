package ch.admin.bit.jeap.governance.secscan.httpcheck;

import ch.admin.bit.jeap.governance.domain.plugin.security.api.HttpEndpoint;
import ch.admin.bit.jeap.governance.secscan.domain.HttpEndpointSecurityChecker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
class DefaultHttpEndpointSecurityChecker implements HttpEndpointSecurityChecker {

    private static final String PARAM_REGEX = "\\{([^}]+)";
    private static final Pattern pattern = Pattern.compile(PARAM_REGEX);

    private final RestClient restClient;

    DefaultHttpEndpointSecurityChecker(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Result check(String apiUrl, HttpEndpoint endpoint) {
        HttpStatusCode statusCode = makeRequest(endpoint.method(), apiUrl + endpoint.path());
        if (statusCode == null) {
            return new Result(false, "Endpoint could not be checked.");
        } else if (isNotSecurityOrTimeoutError(statusCode)) {
            return new Result(true, "Endpoint failed check by returning status %s.".formatted(statusCode));
        } else {
            return new Result(false, "Endpoint passed check by returning status %s.".formatted(statusCode));
        }
    }

    private boolean isNotSecurityOrTimeoutError(HttpStatusCode httpStatusCode) {
        return !(httpStatusCode.isSameCodeAs(HttpStatus.FORBIDDEN)
                || httpStatusCode.isSameCodeAs(HttpStatus.UNAUTHORIZED)
                || httpStatusCode.isSameCodeAs(HttpStatus.BAD_GATEWAY)
                || httpStatusCode.isSameCodeAs(HttpStatus.SERVICE_UNAVAILABLE)
                || httpStatusCode.isSameCodeAs(HttpStatus.GATEWAY_TIMEOUT));
    }

    private HttpStatusCode makeRequest(String method, String urlTemplate) {
        try {
            HttpStatusCode statusCode = restClient
                    .method(HttpMethod.valueOf(method))
                    .uri(urlTemplate, createTestPathVariables(urlTemplate))
                    .exchange((request, response) -> response.getStatusCode());
            log.debug("HTTP check request {} {} returned status {}.", method, urlTemplate, statusCode);
            return statusCode;
        } catch (Exception e) {
            log.warn("HTTP check request {} {} failed.", method, urlTemplate, e);
            return null;
        }
    }

    private Map<String, String> createTestPathVariables(String path) {
        Map<String, String> variables = new HashMap<>();
        Matcher matcher = pattern.matcher(path);
        while (matcher.find()) {
            variables.put(matcher.group(1), "test");
        }
        return variables;
    }

}
