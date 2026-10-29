package ch.admin.bit.jeap.governance.reactionobserver.connector;

import ch.admin.bit.jeap.governance.reactionobserver.ReactionObserverProperties;
import ch.admin.bit.jeap.security.restclient.JeapOAuth2RestClientBuilderFactory;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
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
import java.time.LocalDate;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReactionObserverConnectorIntegrationTest {

    private static final String CLIENT_REGISTRATION = "reaction-observer";
    private static final String ACCESS_TOKEN = "reaction-observer-token";
    private static WireMockServer wireMockServer;
    private ReactionObserverConnector reactionObserverConnector;
    private ObjectMapper objectMapper;
    private JeapOAuth2RestClientBuilderFactory restClientBuilderFactory;


    @Test
    void getAllComponentLastObservationDates_shouldReturnModel() throws Exception {
        stubFor(get(urlEqualTo("/api/statistics/last-observation-date"))
                .withHeader("Authorization", equalTo("Bearer " + ACCESS_TOKEN))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(Map.of("test1", LocalDate.now(), "test2", LocalDate.now().minusDays(1))))));

        Map<String, LocalDate> result = reactionObserverConnector.getAllComponentLastObservationDates();

        assertThat(result)
                .isNotNull()
                .hasSize(2);
        verify(restClientBuilderFactory).createForClientRegistryId(CLIENT_REGISTRATION);
    }

    @Test
    void getAllComponentLastObservationDates_shouldReturnEmptyIfNull() throws Exception {
        stubFor(get(urlEqualTo("/api/statistics/last-observation-date"))
                .withHeader("Authorization", equalTo("Bearer " + ACCESS_TOKEN))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(Map.of()))));

        Map<String, LocalDate> result = reactionObserverConnector.getAllComponentLastObservationDates();

        assertThat(result)
                .isNotNull()
                .isEmpty();
    }

    @Test
    void getAllComponentLastObservationDates_shouldThrowException_when500Error() {
        stubFor(get(urlEqualTo("/api/statistics/last-observation-date"))
                .withHeader("Authorization", equalTo("Bearer " + ACCESS_TOKEN))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        assertThatThrownBy(() -> reactionObserverConnector.getAllComponentLastObservationDates())
                .isInstanceOf(ReactionObserverConnectorException.class)
                .hasCauseInstanceOf(RestClientException.class);
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

        objectMapper = new JsonMapper();

        ReactionObserverProperties properties = new ReactionObserverProperties();
        properties.setUrl(wireMockServer.baseUrl());
        properties.setClientRegistration(CLIENT_REGISTRATION);
        properties.setTimeout(Duration.ofSeconds(5));

        restClientBuilderFactory = mock(JeapOAuth2RestClientBuilderFactory.class);
        when(restClientBuilderFactory.createForClientRegistryId(CLIENT_REGISTRATION))
                .thenReturn(RestClient.builder()
                        .requestInitializer(request -> request.getHeaders().setBearerAuth(ACCESS_TOKEN)));
        reactionObserverConnector = new ReactionObserverConnector(
                restClientBuilderFactory,
                properties
        );
    }
}
