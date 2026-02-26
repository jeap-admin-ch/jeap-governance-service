package ch.admin.bit.jeap.governance.secscan.apidiscovery;

import ch.admin.bit.jeap.governance.domain.GovernanceServiceEnvironment;
import ch.admin.bit.jeap.governance.domain.plugin.security.api.HttpApi;
import ch.admin.bit.jeap.governance.domain.plugin.security.api.HttpEndpoint;
import ch.admin.bit.jeap.governance.domain.plugin.security.api.SystemComponentHttpApi;
import ch.admin.bit.jeap.governance.secscan.domain.SystemComponentHttpApiDiscoveryClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
class ApiDiscoveryClient implements SystemComponentHttpApiDiscoveryClient {

    private final ApiDiscoveryClientConfigurationProperties apiDiscoveryClientConfigProperties;
    private final RestClient restClient;

    ApiDiscoveryClient(RestClient restClient, ApiDiscoveryClientConfigurationProperties apiDiscoveryClientConfigProperties) {
        this.apiDiscoveryClientConfigProperties = apiDiscoveryClientConfigProperties;
        this.restClient = restClient;
    }

    @Override
    public SystemComponentHttpApi discover(String systemComponentName, GovernanceServiceEnvironment environment) {
        RestApiResultDto restApiResultDto = getRestApi(systemComponentName, environment);
        if (restApiResultDto == null) {
            return null;
        }
        return new SystemComponentHttpApi(
                systemComponentName,
                environment,
                toHttpApi(restApiResultDto),
                restApiResultDto.lastUpdated()
        );
    }

    private RestApiResultDto getRestApi(String systemComponentName, GovernanceServiceEnvironment environment) {
        try {
            return restClient.get()
                    .uri(apiDiscoveryClientConfigProperties.getUrlTemplate(),
                            Map.of("systemComponentName", systemComponentName,
                                   "env", environment.name().toLowerCase()))
                    .retrieve()
                    .body(RestApiResultDto.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND || e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                log.debug("Rest API not found for system component '{}'.", systemComponentName);
                return null;
            }
            throw new ApisDiscoveryClientException("Failed to get the REST API description for the system component '%s' because of an HTTP client error.".
                    formatted(systemComponentName), e);
        } catch (ApisDiscoveryClientException adce) {
            throw adce;
        } catch (Exception e) {
            throw new ApisDiscoveryClientException("Failed to get REST API description for system component '%s'".formatted(systemComponentName), e);
        }
    }

    private static HttpApi toHttpApi(RestApiResultDto restApiResultDto) {
        List<HttpEndpoint> endpoints = restApiResultDto.restApis() == null ?
                Collections.emptyList() :
                restApiResultDto.restApis().stream()
                        .map(ApiDiscoveryClient::toEndpoint)
                        .toList();

        return new HttpApi(
                restApiResultDto.serverUrl(),
                restApiResultDto.version(),
                endpoints
        );
    }

    private static HttpEndpoint toEndpoint(RestApiDto restApiDto) {
        return new HttpEndpoint(restApiDto.path(), restApiDto.method());
    }

}
