package ch.admin.bit.jeap.governance.secscan.apidiscovery;

import ch.admin.bit.jeap.governance.domain.GovernanceServiceEnvironment;
import ch.admin.bit.jeap.governance.domain.plugin.security.api.SystemComponentHttpApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class ApiDiscoveryClientTest {

    private static final String URL_TEMPLATE = "http://api-discovery.example.com/api/{env}/{systemComponentName}";
    private static final String COMPONENT_NAME = "mysystem-mycomp-svc";
    private static final GovernanceServiceEnvironment ENV = GovernanceServiceEnvironment.REF;

    private MockRestServiceServer mockServer;
    private ApiDiscoveryClient client;

    @BeforeEach
    void setUp() {
        ApiDiscoveryClientConfigurationProperties properties = new ApiDiscoveryClientConfigurationProperties();
        properties.setUrlTemplate(URL_TEMPLATE);

        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new ApiDiscoveryClient(builder.build(), properties);
    }

    @Test
    void discover_successfulResponse_returnsMappedResult() {
        mockServer.expect(requestTo("http://api-discovery.example.com/api/ref/mysystem-mycomp-svc"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                    "serverUrl": "http://mysystem-mycomp-svc.example.com",
                                    "version": "2.1",
                                    "lastUpdated": "2026-01-15T10:30:00+01:00",
                                    "restApis": [
                                        {"method": "GET", "path": "/api/users"},
                                        {"method": "POST", "path": "/api/users"},
                                        {"method": "DELETE", "path": "/api/users/{id}"}
                                    ]
                                }
                                """));

        SystemComponentHttpApi result = client.discover(COMPONENT_NAME, ENV);

        assertThat(result).isNotNull();
        assertThat(result.systemComponentName()).isEqualTo(COMPONENT_NAME);
        assertThat(result.environment()).isEqualTo(ENV);
        assertThat(result.lastUpdated()).isEqualTo(ZonedDateTime.parse("2026-01-15T10:30:00+01:00"));
        assertThat(result.httpApi().url()).isEqualTo("http://mysystem-mycomp-svc.example.com");
        assertThat(result.httpApi().version()).isEqualTo("2.1");
        assertThat(result.httpApi().endpoints()).hasSize(3);
        assertThat(result.httpApi().endpoints().get(0).method()).isEqualTo("GET");
        assertThat(result.httpApi().endpoints().get(0).path()).isEqualTo("/api/users");
        assertThat(result.httpApi().endpoints().get(1).method()).isEqualTo("POST");
        assertThat(result.httpApi().endpoints().get(1).path()).isEqualTo("/api/users");
        assertThat(result.httpApi().endpoints().get(2).method()).isEqualTo("DELETE");
        assertThat(result.httpApi().endpoints().get(2).path()).isEqualTo("/api/users/{id}");
    }

    @Test
    void discover_responseWithNullRestApis_returnsEmptyEndpoints() {
        mockServer.expect(requestTo("http://api-discovery.example.com/api/ref/mysystem-mycomp-svc"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                    "serverUrl": "http://mysystem-mycomp-svc.example.com",
                                    "version": "1.0"
                                }
                                """));

        SystemComponentHttpApi result = client.discover(COMPONENT_NAME, ENV);

        assertThat(result).isNotNull();
        assertThat(result.httpApi().endpoints()).isEmpty();
    }

    @Test
    void discover_notFound_returnsNull() {
        mockServer.expect(requestTo("http://api-discovery.example.com/api/ref/mysystem-mycomp-svc"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        SystemComponentHttpApi result = client.discover(COMPONENT_NAME, ENV);

        assertThat(result).isNull();
    }

    @Test
    void discover_badRequest_returnsNull() {
        mockServer.expect(requestTo("http://api-discovery.example.com/api/ref/mysystem-mycomp-svc"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        SystemComponentHttpApi result = client.discover(COMPONENT_NAME, ENV);

        assertThat(result).isNull();
    }

    @Test
    void discover_otherClientError_throwsApisDiscoveryClientException() {
        mockServer.expect(requestTo("http://api-discovery.example.com/api/ref/mysystem-mycomp-svc"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> client.discover(COMPONENT_NAME, ENV))
                .isInstanceOf(ApisDiscoveryClientException.class)
                .hasMessageContaining(COMPONENT_NAME);
    }

    @Test
    void discover_serverError_throwsApisDiscoveryClientException() {
        mockServer.expect(requestTo("http://api-discovery.example.com/api/ref/mysystem-mycomp-svc"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.discover(COMPONENT_NAME, ENV))
                .isInstanceOf(ApisDiscoveryClientException.class)
                .hasMessageContaining(COMPONENT_NAME);
    }

    @Test
    void discover_usesLowercaseEnvironmentInUrl() {
        mockServer.expect(requestTo("http://api-discovery.example.com/api/dev/mysystem-mycomp-svc"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        client.discover(COMPONENT_NAME, GovernanceServiceEnvironment.DEV);

        mockServer.verify();
    }
}
