package ch.admin.bit.jeap.governance.deploymentlog.connector;

import ch.admin.bit.jeap.governance.deploymentlog.DeploymentLogProperties;
import ch.admin.bit.jeap.governance.domain.GovernanceServiceEnvironment;
import ch.admin.bit.jeap.governance.deploymentlog.connector.model.DeploymentLogComponentVersionDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeploymentLogConnectorIntegrationTest {

    private static final String USERNAME = "myUsername";
    private static final String PASSWORD = "myPassword";
    private static WireMockServer wireMockServer;
    private DeploymentLogConnector deploymentLogConnector;
    private ObjectMapper objectMapper;


    @Test
    void getAllComponentVersions_shouldReturnModel() throws Exception {
        GovernanceServiceEnvironment governanceServiceEnvironment = GovernanceServiceEnvironment.PROD;
        Set<DeploymentLogComponentVersionDto> dtos = createDeploymentLogComponentVersionDtos();

        stubFor(get(urlEqualTo("/api/environment/prod/components"))
                .withBasicAuth(USERNAME, PASSWORD)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(dtos))));

        Set<DeploymentLogComponentVersionDto> result = deploymentLogConnector.getAllComponentVersions(governanceServiceEnvironment);

        assertNotNull(result);

        assertEquals(2, result.size());
    }

    @Test
    void getAllComponentVersions_shouldReturnEmptyIfNull() throws Exception {
        GovernanceServiceEnvironment governanceServiceEnvironment = GovernanceServiceEnvironment.PROD;
        Set<DeploymentLogComponentVersionDto> dtos = null;

        stubFor(get(urlEqualTo("/api/environment/prod/components"))
                .withBasicAuth(USERNAME, PASSWORD)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(dtos))));

        Set<DeploymentLogComponentVersionDto> result = deploymentLogConnector.getAllComponentVersions(governanceServiceEnvironment);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getModelFromArchRepo_shouldThrowException_when500Error() {
        GovernanceServiceEnvironment governanceServiceEnvironment = GovernanceServiceEnvironment.PROD;
        stubFor(get(urlEqualTo("/api/environment/prod/components"))
                .withBasicAuth(USERNAME, PASSWORD)
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        assertThatThrownBy(() -> deploymentLogConnector.getAllComponentVersions(governanceServiceEnvironment))
                .isInstanceOf(DeploymentLogConnectorException.class)
                .hasCauseInstanceOf(RestClientException.class);
    }


    private Set<DeploymentLogComponentVersionDto> createDeploymentLogComponentVersionDtos() {
        return Set.of(
                new DeploymentLogComponentVersionDto("SystemComponent 1", "1.0.0"),
                new DeploymentLogComponentVersionDto("SystemComponent 2", "1.0.0")
        );
    }

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig()
                .dynamicPort());
        wireMockServer.start();
        WireMock.configureFor(wireMockServer.port());
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();

        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        DeploymentLogProperties properties = new DeploymentLogProperties();
        properties.setUrl(wireMockServer.baseUrl());
        properties.setTimeout(Duration.ofSeconds(5));
        properties.setUsername(USERNAME);
        properties.setPassword(PASSWORD);

        deploymentLogConnector = new DeploymentLogConnector(
                RestClient.builder(),
                properties
        );
    }
}
