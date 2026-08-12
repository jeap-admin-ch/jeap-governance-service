package ch.admin.bit.jeap.governance.messagecontract.connector;

import ch.admin.bit.jeap.governance.messagecontract.MessageContractProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class MessageContractConnector {
    private final RestClient restClient;
    private final MessageContractProperties properties;

    public MessageContractConnector(RestClient.Builder restClientBuilder, MessageContractProperties properties) {
        var requestFactory = ClientHttpRequestFactoryBuilder.simple()
                .withCustomizer(client -> {
                    client.setConnectTimeout(properties.getTimeout());
                    client.setReadTimeout(properties.getTimeout());
                })
                .build();
        this.restClient = restClientBuilder
                .defaultHeaders(headers -> headers.setBasicAuth(properties.getUsername(), properties.getPassword()))
                .requestFactory(requestFactory)
                .build();
        this.properties = properties;
    }

    public List<MessageContractVersionStatusDto> getVersionStatus() {
        try {
            List<MessageContractVersionStatusDto> statuses = restClient.get()
                    .uri(properties.getUrl(), properties.getEnvironment())
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            if (statuses == null) {
                throw new IllegalStateException("Message contract version response body must not be null");
            }
            return statuses;
        } catch (Exception ex) {
            throw new MessageContractConnectorException("Failed to retrieve message contract version status", ex);
        }
    }
}
