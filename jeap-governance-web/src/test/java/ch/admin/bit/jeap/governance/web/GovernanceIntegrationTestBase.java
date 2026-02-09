package ch.admin.bit.jeap.governance.web;

import ch.admin.bit.jeap.governance.archrepo.connector.model.ApiDocVersionDto;
import ch.admin.bit.jeap.governance.archrepo.connector.model.ArchRepoModelDto;
import ch.admin.bit.jeap.governance.archrepo.connector.model.DatabaseSchemaVersionDto;
import ch.admin.bit.jeap.governance.archrepo.connector.model.ReactionGraphDto;
import ch.admin.bit.jeap.governance.archrepo.connector.model.RestApiRelationWithoutPactDto;
import ch.admin.bit.jeap.governance.domain.GovernanceServiceEnvironment;
import ch.admin.bit.jeap.governance.deploymentlog.connector.model.DeploymentLogComponentVersionDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Set;

import static ch.admin.bit.jeap.governance.web.ImportModelHelper.createArchRepoModelDtoOneSystemLessOneSystemComponentEachLess;
import static ch.admin.bit.jeap.governance.web.ImportModelHelper.createDefaultArchRepoModelDto;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

public abstract class GovernanceIntegrationTestBase {

    private static final String DL_USERNAME = "myUsername";
    private static final String DL_PASSWORD = "myPassword";
    private static final GovernanceServiceEnvironment DL_ENVIRONMENT = GovernanceServiceEnvironment.PROD;

    protected static WireMockServer archRepoMockServer;
    protected static WireMockServer deploymentLogMockServer;

    private static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine").asCompatibleSubstituteFor("postgres:16-alpine")
    );

    @Autowired
    protected ObjectMapper objectMapper;

    @BeforeAll
    static void startInfrastructure() {
        archRepoMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig()
                .dynamicPort());
        archRepoMockServer.start();
        WireMock.configureFor(archRepoMockServer.port());
        deploymentLogMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig()
                .dynamicPort());
        deploymentLogMockServer.start();
        WireMock.configureFor(deploymentLogMockServer.port());

        postgres.start();
    }

    @AfterAll
    static void stopInfrastructure() {
        if (archRepoMockServer != null) {
            archRepoMockServer.stop();
        }
        if (deploymentLogMockServer != null) {
            deploymentLogMockServer.stop();
        }
        postgres.stop();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("jeap.governance.environment", DL_ENVIRONMENT::name);

        registry.add("jeap.governance.archrepo.url", archRepoMockServer::baseUrl);

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("jeap.governance.deploymentlog.url", deploymentLogMockServer::baseUrl);
        registry.add("jeap.governance.deploymentlog.username", () -> DL_USERNAME);
        registry.add("jeap.governance.deploymentlog.password", () -> DL_PASSWORD);
    }

    @BeforeEach
    void resetWireMock() {
        archRepoMockServer.resetAll();
    }

    // Helper methods for stubbing

    protected void stubArchRepoModel(ArchRepoModelDto model) throws Exception {
        archRepoMockServer.stubFor(get(urlEqualTo("/api/model"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(model))));
    }

    protected void stubArchRepoApiDocVersions(List<ApiDocVersionDto> apiDocVersionDtos) throws Exception {
        archRepoMockServer.stubFor(get(urlEqualTo("/api/openapi/versions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(apiDocVersionDtos))));
    }

    protected void stubArchRepoDatabaseSchemaVersions(List<DatabaseSchemaVersionDto> databaseSchemaVersionDtos) throws Exception {
        archRepoMockServer.stubFor(get(urlEqualTo("/api/dbschemas/versions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(databaseSchemaVersionDtos))));
    }

    protected void stubArchRepoRestApiRelationsWithoutPact(List<RestApiRelationWithoutPactDto> restApiRelationsWithoutPact) throws Exception {
        archRepoMockServer.stubFor(get(urlEqualTo("/api/model/rest-api-relation-without-pact"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(restApiRelationsWithoutPact))));
    }

    protected void stubArchRepoReactionGraphs(List<ReactionGraphDto> reactionGraphDtos) throws Exception {
        archRepoMockServer.stubFor(get(urlEqualTo("/api/reactions/components"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(reactionGraphDtos))));
    }

    protected void stubDeploymentLogDeploymentLogComponentVersions(Set<DeploymentLogComponentVersionDto> dtos) throws Exception {
        deploymentLogMockServer.stubFor(get(urlEqualTo("/api/environment/prod/components"))
                .withBasicAuth(DL_USERNAME, DL_PASSWORD)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(dtos))));
    }

    protected void setUpImportDefaultModel() throws Exception {
        ArchRepoModelDto modelDto = createDefaultArchRepoModelDto();
        stubArchRepoModel(modelDto);
    }

    protected void setUpImportModelLess() throws Exception {
        ArchRepoModelDto modelDto = createArchRepoModelDtoOneSystemLessOneSystemComponentEachLess();
        stubArchRepoModel(modelDto);
    }
}
