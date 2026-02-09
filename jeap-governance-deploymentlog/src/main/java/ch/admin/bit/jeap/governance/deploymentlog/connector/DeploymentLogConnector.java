package ch.admin.bit.jeap.governance.deploymentlog.connector;

import ch.admin.bit.jeap.governance.deploymentlog.DeploymentLogProperties;
import ch.admin.bit.jeap.governance.domain.GovernanceServiceEnvironment;
import ch.admin.bit.jeap.governance.deploymentlog.connector.model.DeploymentLogComponentVersionDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Set;

/**
 * This class connects to the ArchRepo, fetching various models and data via its REST API.
 */
@Component
@Slf4j
public class DeploymentLogConnector {
    private final RestClient restClient;
    private final String deploymentLogUrl;

    public DeploymentLogConnector(RestClient.Builder restClientBuilder, DeploymentLogProperties deploymentLogProperties) {
        deploymentLogUrl = deploymentLogProperties.getUrl();
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder
                .simple()
                .withCustomizer(c -> c.setConnectTimeout(deploymentLogProperties.getTimeout()))
                .build();
        this.restClient = restClientBuilder
                .baseUrl(deploymentLogUrl)
                .defaultHeaders(headers -> headers.setBasicAuth(deploymentLogProperties.getUsername(), deploymentLogProperties.getPassword()))
                .requestFactory(requestFactory)
                .build();
    }

    public Set<DeploymentLogComponentVersionDto> getAllComponentVersions(GovernanceServiceEnvironment environment) {
        log.info("Retrieving all component versions from the deployment log at URL '{}' for environment '{}'.",
                deploymentLogUrl, environment);
        try {
            Set<DeploymentLogComponentVersionDto> componentVersionDtos = restClient.get()
                    .uri("/api/environment/{environment}/components", environment.name().toLowerCase())
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            return componentVersionDtos != null ? componentVersionDtos : Set.of();
        } catch (Exception e) {
            throw new DeploymentLogConnectorException(e);
        }
    }
}
