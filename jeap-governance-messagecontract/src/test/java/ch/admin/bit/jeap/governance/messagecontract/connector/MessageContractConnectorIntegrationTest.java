package ch.admin.bit.jeap.governance.messagecontract.connector;

import ch.admin.bit.jeap.governance.messagecontract.MessageContractProperties;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.*;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessageContractConnectorIntegrationTest {
    private static WireMockServer server;
    private MessageContractConnector connector;

    @BeforeAll
    static void startServer() {
        server = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        server.start();
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    @BeforeEach
    void setUp() {
        server.resetAll();
        var properties = new MessageContractProperties();
        properties.setUrl(server.baseUrl() + "/api/contracts/version-status?env={environment}");
        properties.setEnvironment("PROD");
        properties.setUsername("reader");
        properties.setPassword("secret");
        properties.setTimeout(Duration.ofSeconds(5));
        connector = new MessageContractConnector(RestClient.builder(), properties);
    }

    @Test
    void retrievesStatusWithBasicAuthentication() {
        server.stubFor(get(urlEqualTo("/api/contracts/version-status?env=PROD"))
                .withBasicAuth("reader", "secret")
                .willReturn(okJson("""
                        [{"appName":"orders-service","appVersion":"2.1.0","messageType":"OrderCreatedEvent",
                        "usedVersion":"1.0.0","latestVersion":"1.1.0","topic":"orders","role":"PRODUCER",
                        "upToDate":false}]
                        """)));

        assertThat(connector.getVersionStatus()).containsExactly(new MessageContractVersionStatusDto(
                "orders-service", "2.1.0", "OrderCreatedEvent", "1.0.0", "1.1.0", "orders", "PRODUCER", false));
    }

    @Test
    void acceptsSuccessfulEmptyResponse() {
        server.stubFor(get(urlPathEqualTo("/api/contracts/version-status")).willReturn(okJson("[]")));

        assertThat(connector.getVersionStatus()).isEmpty();
    }

    @Test
    void rejectsMissingResponseBody() {
        server.stubFor(get(urlPathEqualTo("/api/contracts/version-status")).willReturn(aResponse().withStatus(200)));

        assertThatThrownBy(connector::getVersionStatus)
                .isInstanceOf(MessageContractConnectorException.class)
                .hasRootCauseMessage("Message contract version response body must not be null");
    }

    @Test
    void wrapsHttpFailure() {
        server.stubFor(get(urlPathEqualTo("/api/contracts/version-status")).willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(connector::getVersionStatus).isInstanceOf(MessageContractConnectorException.class);
    }
}
