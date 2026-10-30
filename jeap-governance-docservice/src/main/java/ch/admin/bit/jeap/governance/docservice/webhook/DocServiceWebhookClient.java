package ch.admin.bit.jeap.governance.docservice.webhook;

import ch.admin.bit.jeap.security.restclient.JeapOAuth2RestClientBuilderFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Tells the Doc Service that a scoring run has completed. The call is not retried: a lost call is caught up by the
 * Doc Service's scheduled import.
 */
@Slf4j
public class DocServiceWebhookClient {

    private final RestClient restClient;
    private final String webhookUrl;

    public DocServiceWebhookClient(JeapOAuth2RestClientBuilderFactory restClientBuilderFactory,
                            DocServiceWebhookProperties properties) {
        this.webhookUrl = properties.getUrl();
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder.simple()
                .withCustomizer(factory -> {
                    factory.setConnectTimeout(properties.getTimeout());
                    factory.setReadTimeout(properties.getTimeout());
                })
                .build();
        this.restClient = restClientBuilderFactory.createForClientRegistryId(properties.getClientRegistration())
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * Calls the webhook. A failure is logged as a warning and never thrown.
     *
     * @param scoredAt the recorded time of the completed scoring run
     */
    public void notifyScoringCompleted(OffsetDateTime scoredAt) {
        ScoringCompletedNotification notification = new ScoringCompletedNotification(UUID.randomUUID(), scoredAt);
        try {
            restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(notification)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Notified the Doc Service at '{}' of the scoring run of {} with event ID {}",
                    webhookUrl, scoredAt, notification.eventId());
        } catch (Exception e) {
            log.warn("Could not notify the Doc Service at '{}' of the scoring run of {}: {}. Its scheduled import "
                     + "catches up.", webhookUrl, scoredAt, e.getMessage());
        }
    }

    /**
     * @param eventId  unique per call, so that the Doc Service can ignore a repeated delivery
     * @param scoredAt the recorded time of the completed scoring run
     */
    record ScoringCompletedNotification(UUID eventId, OffsetDateTime scoredAt) {
    }
}
