package ch.admin.bit.jeap.governance.prometheus.amp;

import ch.admin.bit.jeap.governance.prometheus.amp.dto.PrometheusQueryResponseResult;
import ch.admin.bit.jeap.governance.prometheus.domain.PromException;
import ch.admin.bit.jeap.governance.prometheus.domain.PromQueryType;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeriesSample;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.http.*;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.StsClientBuilder;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.Credentials;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static ch.admin.bit.jeap.governance.domain.GovernanceServiceEnvironment.PROD;
import static ch.admin.bit.jeap.governance.domain.GovernanceServiceEnvironment.REF;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AmazonManagedPromClientTest {

    @Mock
    private StsClient stsClient;
    @Mock
    private StsClientBuilder stsClientBuilder;
    @Mock
    private SdkHttpClient httpClient;
    @Mock
    private UrlConnectionHttpClient.Builder httpClientBuilder;
    @Mock
    private ExecutableHttpRequest executableRequest;
    @Mock
    private HttpExecuteResponse httpExecuteResponse;
    @Mock
    private SdkHttpResponse sdkHttpResponse;

    private AmazonManagedPromClient client;

    @BeforeEach
    void setUp() {
        AmazonManagedPromClientProperties properties = new AmazonManagedPromClientProperties();
        properties.setHost("https://aps-workspaces.eu-central-1.amazonaws.com");
        properties.setWorkspace("test-workspace");
        properties.setRoleArn("arn:aws:iam::123456789:role/test-role");
        properties.setRoleSessionName("testSession");
        client = new AmazonManagedPromClient(properties);
    }

    @Test
    void query_successfulResponse_returnsResults() {
        String jsonResponse = """
                {
                    "status": "success",
                    "data": {
                        "resultType": "vector",
                        "result": [
                            {"metric": {"service": "test-service", "stage": "ref", "task_revision": "10"}, "value": ["1234567890", "1"]}
                        ]
                    }
                }
                """;

        try (MockedStatic<StsClient> stsMock = mockStatic(StsClient.class);
             MockedStatic<UrlConnectionHttpClient> httpMock = mockStatic(UrlConnectionHttpClient.class)) {

            setupStsMock(stsMock);
            setupHttpMock(httpMock, jsonResponse);

            List<PromTimeSeriesSample> results = client.query(
                    PromQueryType.JEAP_MESSAGING_CONTRACT, REF, "test-service");

            assertEquals(1, results.size());
            assertEquals("test-service", results.getFirst().metric().get("service"));
            assertEquals("ref", results.getFirst().metric().get("stage"));
        }
    }

    @Test
    void query_multipleTaskRevisions_filtersToMaxRevision() {
        String jsonResponse = """
                {
                    "status": "success",
                    "data": {
                        "resultType": "vector",
                        "result": [
                            {"metric": {"service": "test-service", "stage": "ref", "task_revision": "35"}, "value": ["1234567890", "1"]},
                            {"metric": {"service": "test-service", "stage": "ref", "task_revision": "36"}, "value": ["1234567891", "2"]},
                            {"metric": {"service": "test-service", "stage": "ref", "task_revision": "34"}, "value": ["1234567892", "3"]}
                        ]
                    }
                }
                """;

        try (MockedStatic<StsClient> stsMock = mockStatic(StsClient.class);
             MockedStatic<UrlConnectionHttpClient> httpMock = mockStatic(UrlConnectionHttpClient.class)) {

            setupStsMock(stsMock);
            setupHttpMock(httpMock, jsonResponse);

            List<PromTimeSeriesSample> results = client.query(
                    PromQueryType.JEAP_JAVA_VERSION, REF, "test-service");

            assertEquals(1, results.size());
            assertEquals("36", results.getFirst().metric().get("task_revision"));
            assertEquals("2", results.getFirst().value().get(1));
        }
    }

    @Test
    void filterByMaxTaskRevision_noTaskRevisionLabel_returnsAll() {
        List<PrometheusQueryResponseResult> results = List.of(
                new PrometheusQueryResponseResult(Map.of("service", "svc1"), List.of("123", "1")),
                new PrometheusQueryResponseResult(Map.of("service", "svc2"), List.of("124", "2"))
        );

        List<PrometheusQueryResponseResult> filtered = client.filterByMaxTaskRevision(results);

        assertEquals(2, filtered.size());
    }

    @Test
    void filterByMaxTaskRevision_emptyList_returnsEmpty() {
        List<PrometheusQueryResponseResult> filtered = client.filterByMaxTaskRevision(List.of());
        assertTrue(filtered.isEmpty());
    }

    @Test
    void filterByMaxTaskRevision_nullList_returnsNull() {
        assertNull(client.filterByMaxTaskRevision(null));
    }

    @Test
    void query_dependencyVersion_returnsAllTags() {
        String jsonResponse = """
                {
                    "status": "success",
                    "data": {
                        "resultType": "vector",
                        "result": [
                            {"metric": {"service": "test-service", "stage": "ref", "name": "spring-boot", "version": "3.2.0", "task_revision": "10"}, "value": ["1234567890", "1"]},
                            {"metric": {"service": "test-service", "stage": "ref", "name": "jeap-spring-boot-starter", "version": "5.1.0", "task_revision": "10"}, "value": ["1234567891", "2"]}
                        ]
                    }
                }
                """;

        try (MockedStatic<StsClient> stsMock = mockStatic(StsClient.class);
             MockedStatic<UrlConnectionHttpClient> httpMock = mockStatic(UrlConnectionHttpClient.class)) {
            setupStsMock(stsMock);
            setupHttpMock(httpMock, jsonResponse);

            List<PromTimeSeriesSample> results = client.query(
                    PromQueryType.JEAP_DEPENDENCY_VERSION, REF, "test-service");

            assertEquals(2, results.size());
            assertEquals("spring-boot", results.get(0).metric().get("name"));
            assertEquals("3.2.0", results.get(0).metric().get("version"));
            assertEquals("jeap-spring-boot-starter", results.get(1).metric().get("name"));
            assertEquals("5.1.0", results.get(1).metric().get("version"));
        }
    }

    @Test
    void query_responseWithError_throwsException() {
        String jsonResponse = """
                {
                    "status": "error",
                    "error": "invalid query",
                    "errorType": "bad_data"
                }
                """;

        try (MockedStatic<StsClient> stsMock = mockStatic(StsClient.class);
             MockedStatic<UrlConnectionHttpClient> httpMock = mockStatic(UrlConnectionHttpClient.class)) {

            setupStsMock(stsMock);
            setupHttpMock(httpMock, jsonResponse);

            assertThrows(PromException.class,
                    () -> client.query(PromQueryType.JEAP_JAVA_VERSION, PROD, "service"));
        }
    }

    @Test
    void query_nullResponse_throwsException() {
        try (MockedStatic<StsClient> stsMock = mockStatic(StsClient.class);
             MockedStatic<UrlConnectionHttpClient> httpMock = mockStatic(UrlConnectionHttpClient.class)) {

            setupStsMock(stsMock);
            setupHttpMockWithEmptyBody(httpMock);

            assertThrows(PromException.class,
                    () -> client.query(PromQueryType.JEAP_JAVA_VERSION, PROD, "service"));
        }
    }

    private void setupStsMock(MockedStatic<StsClient> stsMock) {
        stsMock.when(StsClient::builder).thenReturn(stsClientBuilder);
        when(stsClientBuilder.credentialsProvider(any())).thenReturn(stsClientBuilder);
        when(stsClientBuilder.region(any())).thenReturn(stsClientBuilder);
        when(stsClientBuilder.build()).thenReturn(stsClient);

        Credentials credentials = Credentials.builder()
                .accessKeyId("testAccessKey")
                .secretAccessKey("testSecretKey")
                .sessionToken("testToken")
                .expiration(Instant.now().plusSeconds(3600))
                .build();
        AssumeRoleResponse assumeRoleResponse = AssumeRoleResponse.builder()
                .credentials(credentials)
                .build();
        when(stsClient.assumeRole(any(AssumeRoleRequest.class))).thenReturn(assumeRoleResponse);
    }

    private void setupHttpMock(MockedStatic<UrlConnectionHttpClient> httpMock, String responseBody) {
        httpMock.when(UrlConnectionHttpClient::builder).thenReturn(httpClientBuilder);
        when(httpClientBuilder.build()).thenReturn(httpClient);
        when(httpClient.prepareRequest(any())).thenReturn(executableRequest);

        // Use thenAnswer to create a fresh stream for each invocation (important for retries)
        when(httpExecuteResponse.responseBody()).thenAnswer(invocation ->
                Optional.of(AbortableInputStream.create(
                        new ByteArrayInputStream(responseBody.getBytes(StandardCharsets.UTF_8)))));
        when(httpExecuteResponse.httpResponse()).thenReturn(sdkHttpResponse);
        when(sdkHttpResponse.statusText()).thenReturn(Optional.of("OK"));

        try {
            when(executableRequest.call()).thenReturn(httpExecuteResponse);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setupHttpMockWithEmptyBody(MockedStatic<UrlConnectionHttpClient> httpMock) {
        httpMock.when(UrlConnectionHttpClient::builder).thenReturn(httpClientBuilder);
        when(httpClientBuilder.build()).thenReturn(httpClient);
        when(httpClient.prepareRequest(any())).thenReturn(executableRequest);

        when(httpExecuteResponse.responseBody()).thenReturn(Optional.empty());
        when(httpExecuteResponse.httpResponse()).thenReturn(sdkHttpResponse);
        when(sdkHttpResponse.statusText()).thenReturn(Optional.of("OK"));

        try {
            when(executableRequest.call()).thenReturn(httpExecuteResponse);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
