package ch.admin.bit.jeap.governance.prometheus.amp;

import ch.admin.bit.jeap.governance.domain.GovernanceServiceEnvironment;
import ch.admin.bit.jeap.governance.prometheus.amp.dto.PrometheusQueryResponse;
import ch.admin.bit.jeap.governance.prometheus.amp.dto.PrometheusQueryResponseResult;
import ch.admin.bit.jeap.governance.prometheus.domain.PromQueryType;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeriesSample;
import ch.admin.bit.jeap.governance.prometheus.domain.PromClient;
import ch.admin.bit.jeap.governance.prometheus.domain.PromException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.http.*;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.Credentials;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * This Prometheus client can execute predefined queries on an Amazon Managed Prometheus.
 * <p>
 * For the Prometheus Query API <a href="https://prometheus.io/docs/prometheus/latest/querying/api/#querying-metadata">...</a>
 */

@Slf4j
@Component
public class AmazonManagedPromClient implements PromClient {

    private static final int DAYS_TO_IMPORT = 2;

    private static final String QUERY_URL_PATTERN = "%s/workspaces/%s/api/v1/query";

    private final ObjectMapper objectMapper;

    private final AmazonManagedPromClientProperties ampClientProperties;

    private final URI ampUri;

    private final RetryTemplate retryTemplate;

    public static final String STAGE_TAG = "stage";
    private static final String SERVICE_TAG = "service";
    private static final String STATUS_TAG = "status";
    private static final String DATAPOINT_TAG = "datapoint";
    private static final String METHOD_TAG = "method";
    private static final String TASK_REVISION_TAG = "task_revision";

    private static final String JEAP_MESSAGING_CONTRACT_QUERY_PATTERN = "last_over_time(jeap_messaging_contract{" +
            STAGE_TAG + "=\"%s\"," +
            SERVICE_TAG + "=\"%s\"}" +
            "[" + DAYS_TO_IMPORT + "d])";

    private static final String JEAP_JAVA_VERSION_QUERY_PATTERN = "last_over_time(jeap_java_version{" +
            STAGE_TAG + "=\"%s\"," +
            SERVICE_TAG + "=\"%s\"}" +
            "[" + DAYS_TO_IMPORT + "d])";

    private static final String JEAP_DEPENDENCY_VERSION_QUERY_PATTERN = "last_over_time(jeap_dependency_version{" +
            STAGE_TAG + "=\"%s\"," +
            SERVICE_TAG + "=\"%s\"}" +
            "[" + DAYS_TO_IMPORT + "d])";

    private static final String JEAP_REST_ENDPOINT_WITHOUT_JWT_PATTERN = "group by (" + SERVICE_TAG + ", " + DATAPOINT_TAG + ", " + STAGE_TAG + ", " + METHOD_TAG + ")(last_over_time(jeap_rest_endpoint_without_jwt_total{" +
            STAGE_TAG + "=\"%s\"," +
            SERVICE_TAG + "=\"%s\"," +
            STATUS_TAG + "!=\"404\"}" +
            "[" + DAYS_TO_IMPORT + "d]))";

    private static final String JEAP_MESSAGING_TOTAL_QUERY_PATTERN = "last_over_time(jeap_messaging_total{" +
            STAGE_TAG + "=\"%s\"," +
            SERVICE_TAG + "=\"%s\"}" +
            "[" + DAYS_TO_IMPORT + "d])";

    private static final String JDBC_CONNECTIONS_ACTIVE_QUERY_PATTERN = "last_over_time(jdbc_connections_active{" +
            STAGE_TAG + "=\"%s\"," +
            SERVICE_TAG + "=\"%s\"}" +
            "[" + DAYS_TO_IMPORT + "d])";

    // Get latest value of jeap_messaging_signature_required_state for a service in a stage
    // When activating the signature required flag, the query will mark the service as compliant as early as possible.
    // When deactivating the flag, the query will mark the service as non-compliant after DAYS_TO_IMPORT.
    private static final String JEAP_MESSAGING_SIGNATURE_REQUIRED_QUERY_PATTERN =
            "max by (" + STAGE_TAG + ", " + SERVICE_TAG + ") (" +
                    "last_over_time(jeap_messaging_signature_required_state{" +
                    STAGE_TAG + "=\"%s\"," +
                    SERVICE_TAG + "=\"%s\"}" +
                    "[" + DAYS_TO_IMPORT + "d]))";

    AmazonManagedPromClient(AmazonManagedPromClientProperties ampClientProperties) {
        this.objectMapper = new JsonMapper();
        this.ampClientProperties = ampClientProperties;
        try {
            this.ampUri = new URI(String.format(QUERY_URL_PATTERN,
                    ampClientProperties.getHost(),
                    ampClientProperties.getWorkspace()));
        } catch (URISyntaxException e) {
            throw PromException.uriProblem(e);
        }

        this.retryTemplate = RetryTemplate.builder()
                .maxAttempts(5)
                .retryOn(Exception.class)
                .fixedBackoff(600)
                .build();
    }

    @Timed("jeap.governance.service.prometheus.queries")
    public List<PromTimeSeriesSample> query(PromQueryType queryType, GovernanceServiceEnvironment environment, String... args) {
        final String queryString = getQueryString(queryType, environment, args);
        PrometheusQueryResponse response;
        try {
            response = callAmp(queryParameters(queryString));
        } catch (Exception e) {
            log.warn("Error in Prometheus call", e);
            throw PromException.connectionFailed(e);
        }
        if (response == null) {
            throw PromException.noSample();
        }
        if (StringUtils.hasText(response.getError())) {
            log.warn("Error in Prometheus call {}", response.getError());
            throw PromException.errorResponse(response.getError());
        }
        List<PrometheusQueryResponseResult> result = filterByMaxTaskRevision(response.getData().getResult());
        return toSamples(result);
    }

    /**
     * Filters results to only include time series with the maximum task_revision label value.
     * This ensures we only get data from the latest ECS task revision.
     */
    List<PrometheusQueryResponseResult> filterByMaxTaskRevision(List<PrometheusQueryResponseResult> results) {
        if (results == null || results.isEmpty()) {
            return results;
        }

        int maxRevision = results.stream()
                .map(r -> r.getMetric().get(TASK_REVISION_TAG))
                .filter(Objects::nonNull)
                .mapToInt(rev -> {
                    try {
                        return Integer.parseInt(rev);
                    } catch (NumberFormatException _) {
                        return Integer.MIN_VALUE;
                    }
                })
                .max()
                .orElse(Integer.MIN_VALUE);

        if (maxRevision == Integer.MIN_VALUE) {
            return results;
        }

        String maxRevisionStr = String.valueOf(maxRevision);
        return results.stream()
                .filter(r -> maxRevisionStr.equals(r.getMetric().get(TASK_REVISION_TAG)))
                .toList();
    }

    private static MultiValueMap<String, String> queryParameters(String queryString) {
        MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
        query.add("query", queryString);
        return query;
    }

    private AwsSessionCredentials retrieveAwsSessionCredentials() {
        log.trace("Call AssumeRole with roleArn {}", ampClientProperties.getRoleArn());

        AssumeRoleResponse assumeRoleResponse;
        try (StsClient stsClient = StsClient.builder()
                .credentialsProvider(null)
                .region(Region.EU_CENTRAL_1)
                .build()) {

            AssumeRoleRequest assumeRoleRequest = AssumeRoleRequest.builder()
                    .roleArn(ampClientProperties.getRoleArn())
                    .roleSessionName(ampClientProperties.getRoleSessionName())
                    .build();

            assumeRoleResponse = stsClient.assumeRole(assumeRoleRequest);
        }

        Credentials sessionCredentials = assumeRoleResponse.credentials();
        return AwsSessionCredentials.create(
                sessionCredentials.accessKeyId(),
                sessionCredentials.secretAccessKey(),
                sessionCredentials.sessionToken());
    }

    private PrometheusQueryResponse callAmp(MultiValueMap<String, String> queryParameters) {
        return retryTemplate.execute(context -> {
            if (context.getRetryCount() > 0) {
                log.warn("Querying Prometheus failed, starting attempt number {}", context.getRetryCount());
            }

            // Create HTTP request
            SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                    .method(SdkHttpMethod.POST)
                    .uri(ampUri)
                    .rawQueryParameters(queryParameters)
                    .build();

            // Sign the request
            Aws4Signer signer = Aws4Signer.create();
            Aws4SignerParams signerParams = Aws4SignerParams.builder()
                    .awsCredentials(retrieveAwsSessionCredentials())
                    .signingName("aps")
                    .signingRegion(Region.EU_CENTRAL_1)
                    .build();

            SdkHttpFullRequest signedRequest = signer.sign(request, signerParams);
            HttpExecuteRequest httpExecuteRequest = HttpExecuteRequest.builder()
                    .request(signedRequest)
                    .build();

            HttpExecuteResponse httpExecuteResponse;
            try (SdkHttpClient httpClient = UrlConnectionHttpClient.builder().build()) {
                httpExecuteResponse = httpClient
                        .prepareRequest(httpExecuteRequest)
                        .call();
            } catch (IOException e) {
                log.warn("Error in Prometheus call", e);
                throw PromException.connectionFailed(e);
            }

            SdkHttpResponse sdkHttpResponse = httpExecuteResponse.httpResponse();
            log.trace("Response Code: {}", sdkHttpResponse.statusText());

            if (httpExecuteResponse.responseBody().isPresent()) {
                try {
                    return getFromResponse(httpExecuteResponse.responseBody().get());
                } catch (IOException e) {
                    log.warn("Error in parsing Prometheus response", e);
                    throw PromException.connectionFailed(e);
                }
            }
            return null;
        });
    }

    private PrometheusQueryResponse getFromResponse(InputStream inputStream) throws IOException {
        String response = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        log.trace("response {}", response);
        PrometheusQueryResponse prometheusQueryResponse = objectMapper.readValue(response, PrometheusQueryResponse.class);
        log.trace("Results {}", prometheusQueryResponse.getData().getResult());
        return prometheusQueryResponse;
    }

    private String getQueryString(PromQueryType queryType, GovernanceServiceEnvironment environment, String... args) {
        final String ampEnv = toAmpEnvironment(environment);
        return switch (queryType) {
            case JEAP_MESSAGING_CONTRACT -> JEAP_MESSAGING_CONTRACT_QUERY_PATTERN.formatted(ampEnv, args[0]);
            case JEAP_JAVA_VERSION -> JEAP_JAVA_VERSION_QUERY_PATTERN.formatted(ampEnv, args[0]);
            case JEAP_DEPENDENCY_VERSION -> JEAP_DEPENDENCY_VERSION_QUERY_PATTERN.formatted(ampEnv, args[0]);
            case JEAP_REST_ENDPOINT_WITHOUT_JWT -> JEAP_REST_ENDPOINT_WITHOUT_JWT_PATTERN.formatted(ampEnv, args[0]);
            case JEAP_MESSAGING_TOTAL -> JEAP_MESSAGING_TOTAL_QUERY_PATTERN.formatted(ampEnv, args[0]);
            case JDBC_CONNECTIONS_ACTIVE -> JDBC_CONNECTIONS_ACTIVE_QUERY_PATTERN.formatted(ampEnv, args[0]);
            case JEAP_MESSAGING_SIGNATURE_REQUIRED -> JEAP_MESSAGING_SIGNATURE_REQUIRED_QUERY_PATTERN.formatted(ampEnv, args[0]);
        };
    }

    private String toAmpEnvironment(GovernanceServiceEnvironment environment) {
        return environment.name().toLowerCase();
    }

    private List<PromTimeSeriesSample> toSamples(List<PrometheusQueryResponseResult> results) {
        return results.stream()
                .filter(Objects::nonNull)
                .map(this::toSample).toList();
    }

    private PromTimeSeriesSample toSample(PrometheusQueryResponseResult result) {
        return new PromTimeSeriesSample(result.getMetric(), result.getValue());
    }
}
