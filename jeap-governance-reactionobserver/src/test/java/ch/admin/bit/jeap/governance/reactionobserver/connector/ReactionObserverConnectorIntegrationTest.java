package ch.admin.bit.jeap.governance.reactionobserver.connector;

import ch.admin.bit.jeap.governance.reactionobserver.ReactionObserverProperties;
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

class ReactionObserverConnectorIntegrationTest {

    private static final String USERNAME = "myUsername";
    private static final String PASSWORD = "myPassword";
    private static WireMockServer wireMockServer;
    private ReactionObserverConnector reactionObserverConnector;
    private ObjectMapper objectMapper;


    @Test
    void getAllComponentLastObservationDates_shouldReturnModel() throws Exception {
        stubFor(get(urlEqualTo("/api/statistics/last-observation-date"))
                .withBasicAuth(USERNAME, PASSWORD)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(Map.of("test1", LocalDate.now(), "test2", LocalDate.now().minusDays(1))))));

        Map<String, LocalDate> result = reactionObserverConnector.getAllComponentLastObservationDates();

        assertThat(result)
                .isNotNull()
                .hasSize(2);
    }

    @Test
    void getAllComponentLastObservationDates_shouldReturnEmptyIfNull() throws Exception {
        stubFor(get(urlEqualTo("/api/statistics/last-observation-date"))
                .withBasicAuth(USERNAME, PASSWORD)
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
                .withBasicAuth(USERNAME, PASSWORD)
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
        properties.setTimeout(Duration.ofSeconds(5));
        properties.setUsername(USERNAME);
        properties.setPassword(PASSWORD);

        reactionObserverConnector = new ReactionObserverConnector(
                RestClient.builder(),
                properties
        );
    }
}
