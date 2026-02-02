package ch.admin.bit.jeap.governance.archrepo.connector;

import ch.admin.bit.jeap.governance.archrepo.ArchRepoProperties;
import ch.admin.bit.jeap.governance.archrepo.connector.model.*;
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
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ArchRepoConnectorIntegrationTest {

    private static WireMockServer wireMockServer;
    private ArchRepoConnector archRepoConnector;
    private ObjectMapper objectMapper;

    @Test
    void getModelFromArchRepo_shouldReturnModel() throws Exception {
        ArchRepoModelDto archRepoModelDto = createArchRepoModel();

        stubFor(get(urlEqualTo("/api/model"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(archRepoModelDto))));

        ArchRepoModelDto result = archRepoConnector.getModelFromArchRepo();

        assertNotNull(result);
        List<ArchRepoSystemDto> systems = result.getSystems();
        assertEquals(1, systems.size());
        assertEquals(2, systems.getFirst().getSystemComponents().size());
    }

    @Test
    void getModelFromArchRepo_shouldThrowException_when500Error() {
        stubFor(get(urlEqualTo("/api/model"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        assertThatThrownBy(() -> archRepoConnector.getModelFromArchRepo())
                .isInstanceOf(ArchRepoConnectorException.class)
                .hasCauseInstanceOf(RestClientException.class);
    }

    @Test
    void getRestRelationWithoutPact_shouldReturnModel() throws Exception {
        List<RestApiRelationWithoutPactDto> restApiRelationDtos = Arrays.asList(
                new RestApiRelationWithoutPactDto("consumerSystemName1", "consumer1", "providerSystemName1", "provider1", "GET", "/api/v2/resource1"),
                new RestApiRelationWithoutPactDto("consumerSystemName2", "consumer2", "providerSystemName2", "provider2", "GET", "/api/v2/resource2")
        );

        stubFor(get(urlEqualTo("/api/model/rest-api-relation-without-pact"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(restApiRelationDtos))));

        List<RestApiRelationWithoutPactDto> result = archRepoConnector.getRestRelationWithoutPact();

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void getRestRelationWithoutPact_shouldThrowException_when500Error() {
        stubFor(get(urlEqualTo("/api/model/rest-api-relation-without-pact"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        assertThatThrownBy(() -> archRepoConnector.getRestRelationWithoutPact())
                .isInstanceOf(ArchRepoConnectorException.class)
                .hasCauseInstanceOf(RestClientException.class);
    }

    @Test
    void getApiDocVersions_shouldReturnModel() throws Exception {
        List<ApiDocVersionDto> apiDocVersions = Arrays.asList(
                new ApiDocVersionDto("System A", "component1", "1.0.0"),
                new ApiDocVersionDto("System B", "component2", "2.0.0")
        );

        stubFor(get(urlEqualTo("/api/openapi/versions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(apiDocVersions))));

        List<ApiDocVersionDto> result = archRepoConnector.getApiDocVersions();

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void getApiDocVersions_shouldReturnEmptyModelWhenNull() throws Exception {
        List<ApiDocVersionDto> apiDocVersions = null;

        stubFor(get(urlEqualTo("/api/openapi/versions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(apiDocVersions))));

        List<ApiDocVersionDto> result = archRepoConnector.getApiDocVersions();

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void getApiDocVersions_shouldThrowException_when500Error() {
        stubFor(get(urlEqualTo("/api/openapi/versions"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        assertThatThrownBy(() -> archRepoConnector.getApiDocVersions())
                .isInstanceOf(ArchRepoConnectorException.class)
                .hasCauseInstanceOf(RestClientException.class);
    }

    @Test
    void getDatabaseSchemaVersions_shouldReturnModel() throws Exception {
        List<DatabaseSchemaVersionDto> databaseSchemaVersionDtos = Arrays.asList(
                new DatabaseSchemaVersionDto("System A", "component1", "1.0.0"),
                new DatabaseSchemaVersionDto("System B", "component2", "2.0.0")
        );

        stubFor(get(urlEqualTo("/api/dbschemas/versions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(databaseSchemaVersionDtos))));

        List<DatabaseSchemaVersionDto> result = archRepoConnector.getDatabaseSchemaVersions();

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void getDatabaseSchemaVersions_shouldReturnEmptyModelWhenNull() throws Exception {
        List<DatabaseSchemaVersionDto> databaseSchemaVersionDtos = null;

        stubFor(get(urlEqualTo("/api/dbschemas/versions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(databaseSchemaVersionDtos))));

        List<DatabaseSchemaVersionDto> result = archRepoConnector.getDatabaseSchemaVersions();

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void getDatabaseSchemaVersions_shouldThrowException_when500Error() {
        stubFor(get(urlEqualTo("/api/dbschemas/versions"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        assertThatThrownBy(() -> archRepoConnector.getDatabaseSchemaVersions())
                .isInstanceOf(ArchRepoConnectorException.class)
                .hasCauseInstanceOf(RestClientException.class);
    }

    @Test
    void getReactionGraph_Dtos_shouldReturnModel() throws Exception {
        List<ReactionGraphDto> reactionGraphDtos = Arrays.asList(
                new ReactionGraphDto("component1", ZonedDateTime.now().minus(2, ChronoUnit.DAYS)),
                new ReactionGraphDto("component2", ZonedDateTime.now().minus(10, ChronoUnit.HOURS))
        );

        stubFor(get(urlEqualTo("/api/reactions/components"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(reactionGraphDtos))));

        List<ReactionGraphDto> result = archRepoConnector.getReactionGraphDtos();

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void getReactionGraph_Dtos_shouldReturnEmptyModelWhenNull() throws Exception {
        List<ReactionGraphDto> reactionGraphDtos = null;

        stubFor(get(urlEqualTo("/api/reactions/components"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(reactionGraphDtos))));

        List<ReactionGraphDto> result = archRepoConnector.getReactionGraphDtos();

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void getReactionGraph_Dtos_shouldThrowException_when500Error() {
        stubFor(get(urlEqualTo("/api/reactions/components"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        assertThatThrownBy(() -> archRepoConnector.getReactionGraphDtos())
                .isInstanceOf(ArchRepoConnectorException.class)
                .hasCauseInstanceOf(RestClientException.class);
    }

    private ArchRepoModelDto createArchRepoModel() {
        var archRepoSystem = ArchRepoSystemDto.builder()
                .name("System A")
                .aliases(Set.of("SysA", "System Alpha"))
                .systemComponents(Arrays.asList(
                        ArchRepoSystemComponentDto.builder()
                                .name("Component 1")
                                .type(ArchRepoSystemComponentType.BACKEND_SERVICE)
                                .build(),
                        ArchRepoSystemComponentDto.builder()
                                .name("Component 2")
                                .type(ArchRepoSystemComponentType.SELF_CONTAINED_SYSTEM)
                                .build()
                ))
                .build();

        return ArchRepoModelDto.builder()
                .systems(List.of(archRepoSystem))
                .build();
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

        ArchRepoProperties properties = new ArchRepoProperties();
        properties.setUrl(wireMockServer.baseUrl());
        properties.setTimeout(Duration.ofSeconds(5));

        archRepoConnector = new ArchRepoConnector(
                RestClient.builder(),
                properties
        );
    }
}
