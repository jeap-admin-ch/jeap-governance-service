package ch.admin.bit.jeap.governance.web;

import ch.admin.bit.jeap.governance.archrepo.connector.model.ApiDocVersionDto;
import ch.admin.bit.jeap.governance.archrepo.connector.model.ArchRepoModelDto;
import ch.admin.bit.jeap.governance.archrepo.connector.model.DatabaseSchemaVersionDto;
import ch.admin.bit.jeap.governance.archrepo.connector.model.ReactionGraphDto;
import ch.admin.bit.jeap.governance.archrepo.connector.model.RestApiRelationWithoutPactDto;
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

import java.util.List;

import static ch.admin.bit.jeap.governance.web.ImportModelHelper.createArchRepoModelDtoOneSystemLessOneSystemComponentEachLess;
import static ch.admin.bit.jeap.governance.web.ImportModelHelper.createDefaultArchRepoModelDto;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

public abstract class ArchRepoMockIntegrationTestBase {

    protected static WireMockServer wireMockServer;

    @Autowired
    protected ObjectMapper objectMapper;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig()
                .dynamicPort());
        wireMockServer.start();
        WireMock.configureFor(wireMockServer.port());
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @DynamicPropertySource
    static void configureArchRepoUrl(DynamicPropertyRegistry registry) {
        registry.add("jeap.governance.archrepo.url", wireMockServer::baseUrl);
    }

    @BeforeEach
    void resetWireMock() {
        wireMockServer.resetAll();
    }

    // Helper methods for stubbing

    protected void stubArchRepoModel(ArchRepoModelDto model) throws Exception {
        stubFor(get(urlEqualTo("/api/model"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(model))));
    }

    protected void stubApiDocVersions(List<ApiDocVersionDto> apiDocVersionDtos) throws Exception {
        stubFor(get(urlEqualTo("/api/openapi/versions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(apiDocVersionDtos))));
    }

    protected void stubDatabaseSchemaVersions(List<DatabaseSchemaVersionDto> databaseSchemaVersionDtos) throws Exception {
        stubFor(get(urlEqualTo("/api/dbschemas/versions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(databaseSchemaVersionDtos))));
    }

    protected void stubRestApiRelationsWithoutPact(List<RestApiRelationWithoutPactDto> restApiRelationsWithoutPact) throws Exception {
        stubFor(get(urlEqualTo("/api/model/rest-api-relation-without-pact"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(restApiRelationsWithoutPact))));
    }

    protected void stubReactionGraphs(List<ReactionGraphDto> reactionGraphDtos) throws Exception {
        stubFor(get(urlEqualTo("/api/reactions/components"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(reactionGraphDtos))));
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
