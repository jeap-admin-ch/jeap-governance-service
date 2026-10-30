package ch.admin.bit.jeap.governance.docservice.webhook;

import ch.admin.bit.jeap.security.restclient.JeapOAuth2RestClientBuilderFactory;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocServiceWebhookClientTest {

    private static final String CLIENT_REGISTRATION = "doc-service-webhook";
    private static final String ACCESS_TOKEN = "governance-notifications-token";
    private static final String WEBHOOK_PATH = "/doc-service/api/governance/webhook";
    private static final OffsetDateTime SCORED_AT = OffsetDateTime.of(2026, 10, 29, 10, 47, 12, 0, ZoneOffset.ofHours(1));

    private WireMockServer docService;
    private JeapOAuth2RestClientBuilderFactory restClientBuilderFactory;

    @BeforeEach
    void startDocService() {
        docService = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        docService.start();
        restClientBuilderFactory = mock(JeapOAuth2RestClientBuilderFactory.class);
        when(restClientBuilderFactory.createForClientRegistryId(CLIENT_REGISTRATION))
                .thenReturn(RestClient.builder()
                        .requestInitializer(request -> request.getHeaders().setBearerAuth(ACCESS_TOKEN)));
    }

    @AfterEach
    void stopDocService() {
        docService.stop();
    }

    @Test
    void notifyScoringCompleted_postsTheEventWithTheTokenOfTheClientRegistration() {
        docService.stubFor(post(WEBHOOK_PATH).willReturn(aResponse().withStatus(202)));

        client(docService.baseUrl() + WEBHOOK_PATH).notifyScoringCompleted(SCORED_AT);

        verify(restClientBuilderFactory).createForClientRegistryId(CLIENT_REGISTRATION);
        List<LoggedRequest> requests = docService.findAll(postRequestedFor(urlEqualTo(WEBHOOK_PATH))
                .withHeader("Authorization", equalTo("Bearer " + ACCESS_TOKEN))
                .withHeader("Content-Type", containing("application/json")));
        assertThat(requests).hasSize(1);
        JsonNode payload = JsonMapper.builder().build().readTree(requests.getFirst().getBodyAsString());
        assertThat(payload.propertyNames()).containsExactlyInAnyOrder("eventId", "scoredAt");
        assertThat(UUID.fromString(payload.get("eventId").asString())).isNotNull();
        assertThat(OffsetDateTime.parse(payload.get("scoredAt").asString())).isEqualTo(SCORED_AT);
    }

    @Test
    void notifyScoringCompleted_sendsANewEventIdEveryTime() {
        docService.stubFor(post(WEBHOOK_PATH).willReturn(aResponse().withStatus(202)));
        DocServiceWebhookClient client = client(docService.baseUrl() + WEBHOOK_PATH);

        client.notifyScoringCompleted(SCORED_AT);
        client.notifyScoringCompleted(SCORED_AT);

        List<String> eventIds = docService.findAll(postRequestedFor(urlEqualTo(WEBHOOK_PATH))).stream()
                .map(request -> JsonMapper.builder().build().readTree(request.getBodyAsString()).get("eventId").asString())
                .toList();
        assertThat(eventIds).hasSize(2).doesNotHaveDuplicates();
    }

    @Test
    void notifyScoringCompleted_anErrorOfTheDocService_isNotThrown() {
        docService.stubFor(post(WEBHOOK_PATH).willReturn(aResponse().withStatus(503)));

        assertThatCode(() -> client(docService.baseUrl() + WEBHOOK_PATH).notifyScoringCompleted(SCORED_AT))
                .doesNotThrowAnyException();
    }

    @Test
    void notifyScoringCompleted_anUnreachableDocService_isNotThrown() {
        String url = docService.baseUrl() + WEBHOOK_PATH;
        docService.stop();

        assertThatCode(() -> client(url).notifyScoringCompleted(SCORED_AT)).doesNotThrowAnyException();
    }

    private DocServiceWebhookClient client(String url) {
        DocServiceWebhookProperties properties = new DocServiceWebhookProperties();
        properties.setEnabled(true);
        properties.setUrl(url);
        properties.setClientRegistration(CLIENT_REGISTRATION);
        properties.setTimeout(Duration.ofSeconds(2));
        return new DocServiceWebhookClient(restClientBuilderFactory, properties);
    }
}
