package ch.admin.bit.jeap.governance.archrepo.connector;

import ch.admin.bit.jeap.governance.archrepo.ArchRepoProperties;
import ch.admin.bit.jeap.governance.archrepo.connector.model.ApiDocVersionDto;
import ch.admin.bit.jeap.governance.archrepo.connector.model.ArchRepoModelDto;
import ch.admin.bit.jeap.governance.archrepo.connector.model.DatabaseSchemaVersionDto;
import ch.admin.bit.jeap.governance.archrepo.connector.model.ReactionGraphDto;
import ch.admin.bit.jeap.governance.archrepo.connector.model.RestApiRelationWithoutPactDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * This class connects to the ArchRepo, fetching various models and data via its REST API.
 */
@Component
@Slf4j
public class ArchRepoConnector {
    private final RestClient restClient;

    public ArchRepoConnector(RestClient.Builder restClientBuilder, ArchRepoProperties archRepoProperties) {
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder
                .simple()
                .withCustomizer(c -> c.setConnectTimeout(archRepoProperties.getTimeout()))
                .build();
        this.restClient = restClientBuilder
                .baseUrl(archRepoProperties.getUrl())
                .requestFactory(requestFactory)
                .build();
    }

    public ArchRepoModelDto getModelFromArchRepo() {
        try {
            return restClient.get()
                    .uri("/api/model")
                    .retrieve()
                    .body(ArchRepoModelDto.class);
        } catch (Exception e) {
            throw new ArchRepoConnectorException(e);
        }
    }

    public List<RestApiRelationWithoutPactDto> getRestRelationWithoutPact() {
        try {
            return restClient.get()
                    .uri("/api/model/rest-api-relation-without-pact")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (Exception e) {
            throw new ArchRepoConnectorException(e);
        }
    }

    public List<ApiDocVersionDto> getApiDocVersions() {
        try {
            List<ApiDocVersionDto> apiDocVersions = restClient.get()
                    .uri("/api/openapi/versions")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            return apiDocVersions != null ? apiDocVersions : List.of();
        } catch (Exception e) {
            throw new ArchRepoConnectorException(e);
        }
    }

    public List<DatabaseSchemaVersionDto> getDatabaseSchemaVersions() {
        try {
            List<DatabaseSchemaVersionDto> databaseSchemaVersionDtos = restClient.get()
                    .uri("/api/dbschemas/versions")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            return databaseSchemaVersionDtos != null ? databaseSchemaVersionDtos : List.of();
        } catch (Exception e) {
            throw new ArchRepoConnectorException(e);
        }
    }

    public List<ReactionGraphDto> getReactionGraphDtos() {
        try {
            List<ReactionGraphDto> components = restClient.get()
                    .uri("/api/reactions/components")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            return components != null ? components : List.of();
        } catch (Exception e) {
            throw new ArchRepoConnectorException(e);
        }
    }
}
