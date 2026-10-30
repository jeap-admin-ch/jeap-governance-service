package ch.admin.bit.jeap.governance.docservice.webhook;

import ch.admin.bit.jeap.governance.domain.scheduler.SchedulerRunRepository;
import ch.admin.bit.jeap.governance.rules.ScoringScheduler;
import ch.admin.bit.jeap.governance.web.GovernanceApplication;
import ch.admin.bit.jeap.governance.web.GovernanceIntegrationTestBase;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * The webhook after a scoring run, on an instance that switched it on: called with the token of the configured
 * client registration and the time the run was recorded, and never failing the scoring.
 */
@SpringBootTest(classes = GovernanceApplication.class, properties = {
        "jeap.governance.doc-service.webhook.enabled=true",
        "jeap.governance.doc-service.webhook.client-registration=doc-service-webhook",
        "jeap.governance.scoring.lock-at-least=PT0S"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DocServiceWebhookIT extends GovernanceIntegrationTestBase {

    private static final String WEBHOOK_PATH = "/doc-service/api/governance/webhook";
    private static final String ACCESS_TOKEN = "governance-notifications-token";
    private static final WireMockServer DOC_SERVICE = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());

    static {
        DOC_SERVICE.start();
    }

    @Autowired
    private ScoringScheduler scoringScheduler;
    @Autowired
    private SchedulerRunRepository schedulerRunRepository;

    @DynamicPropertySource
    static void docServiceProperties(DynamicPropertyRegistry registry) {
        registry.add("jeap.governance.doc-service.webhook.url", () -> DOC_SERVICE.baseUrl() + WEBHOOK_PATH);
        registry.add("spring.security.oauth2.client.registration.doc-service-webhook.provider", () -> "doc-service-webhook");
        registry.add("spring.security.oauth2.client.registration.doc-service-webhook.client-id", () -> "governance-service");
        registry.add("spring.security.oauth2.client.registration.doc-service-webhook.client-secret", () -> "client-secret");
        registry.add("spring.security.oauth2.client.registration.doc-service-webhook.authorization-grant-type", () -> "client_credentials");
        registry.add("spring.security.oauth2.client.provider.doc-service-webhook.token-uri", () -> DOC_SERVICE.baseUrl() + "/oauth/token");
    }

    @AfterAll
    static void stopDocService() {
        DOC_SERVICE.stop();
    }

    @BeforeEach
    void resetDocService() {
        DOC_SERVICE.resetAll();
        DOC_SERVICE.stubFor(post(urlEqualTo("/oauth/token")).willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                        {"access_token":"%s","token_type":"Bearer","expires_in":3600}
                        """.formatted(ACCESS_TOKEN))));
    }

    @Test
    void aCompletedScoringRun_callsTheWebhook() {
        DOC_SERVICE.stubFor(post(urlEqualTo(WEBHOOK_PATH)).willReturn(aResponse().withStatus(202)));

        scoringScheduler.updateScores();

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(DOC_SERVICE.findAll(postRequestedFor(urlEqualTo(WEBHOOK_PATH))
                        .withHeader("Authorization", equalTo("Bearer " + ACCESS_TOKEN)))).hasSize(1));
        JsonNode payload = objectMapper.readTree(DOC_SERVICE.findAll(postRequestedFor(urlEqualTo(WEBHOOK_PATH)))
                .getFirst().getBodyAsString());
        LocalDateTime recordedRun = schedulerRunRepository.findLastRunDateTime("scoring").orElseThrow();
        assertThat(OffsetDateTime.parse(payload.get("scoredAt").asString()))
                .isEqualTo(recordedRun.atZone(ZoneId.systemDefault()).toOffsetDateTime());
        assertThat(payload.get("eventId").asString()).isNotBlank();
    }

    @Test
    void aFailingWebhook_doesNotFailTheScoring() {
        DOC_SERVICE.stubFor(post(urlEqualTo(WEBHOOK_PATH)).willReturn(aResponse().withStatus(500)));
        LocalDateTime before = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        scoringScheduler.updateScores();

        assertThat(schedulerRunRepository.findLastRunDateTime("scoring")).hasValueSatisfying(lastRun ->
                assertThat(lastRun).isAfterOrEqualTo(before));
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(DOC_SERVICE.findAll(postRequestedFor(urlEqualTo(WEBHOOK_PATH)))).hasSize(1));
    }
}
