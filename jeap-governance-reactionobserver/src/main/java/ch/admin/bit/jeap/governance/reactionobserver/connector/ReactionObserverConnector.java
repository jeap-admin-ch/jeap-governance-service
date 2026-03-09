package ch.admin.bit.jeap.governance.reactionobserver.connector;

import ch.admin.bit.jeap.governance.reactionobserver.ReactionObserverProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.Map;

@Component
@Slf4j
public class ReactionObserverConnector {
    private final RestClient restClient;
    private final String reactionObserverUrl;

    public ReactionObserverConnector(RestClient.Builder restClientBuilder, ReactionObserverProperties properties) {
        reactionObserverUrl = properties.getUrl();
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder
                .simple()
                .withCustomizer(c -> c.setConnectTimeout(properties.getTimeout()))
                .build();
        this.restClient = restClientBuilder
                .baseUrl(reactionObserverUrl)
                .defaultHeaders(headers -> headers.setBasicAuth(properties.getUsername(), properties.getPassword()))
                .requestFactory(requestFactory)
                .build();
    }

    public Map<String, LocalDate> getAllComponentLastObservationDates() {
        log.info("Retrieving all component LastObservationDate from the reaction observer at URL '{}'", reactionObserverUrl);
        try {
            Map<String, LocalDate> componentLastObservationDates = restClient.get()
                    .uri("/api/statistics/last-observation-date")
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            return componentLastObservationDates != null ? componentLastObservationDates : Map.of();
        } catch (Exception e) {
            throw new ReactionObserverConnectorException(e);
        }
    }
}
