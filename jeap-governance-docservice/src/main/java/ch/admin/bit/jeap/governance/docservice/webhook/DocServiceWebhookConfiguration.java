package ch.admin.bit.jeap.governance.docservice.webhook;

import ch.admin.bit.jeap.security.restclient.JeapOAuth2RestClientBuilderFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * The webhook call to the Doc Service, present only while {@code jeap.governance.doc-service.webhook.enabled} is on.
 * Refuses to start without the webhook URL and client registration, naming the missing settings.
 */
@Configuration
@ConditionalOnProperty(name = "jeap.governance.doc-service.webhook.enabled", havingValue = "true")
@EnableConfigurationProperties(DocServiceWebhookProperties.class)
class DocServiceWebhookConfiguration {

    private static final String PREFIX = "jeap.governance.doc-service.webhook.";

    DocServiceWebhookConfiguration(DocServiceWebhookProperties properties) {
        List<String> missing = new ArrayList<>();
        if (!StringUtils.hasText(properties.getUrl())) {
            missing.add(PREFIX + "url");
        }
        if (!StringUtils.hasText(properties.getClientRegistration())) {
            missing.add(PREFIX + "client-registration");
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("The Doc Service webhook is enabled with '" + PREFIX + "enabled', but "
                                            + String.join(" and ", missing) + " is not set. Configure it, or "
                                            + "switch the webhook off.");
        }
    }

    @Bean
    DocServiceWebhookClient docServiceWebhookClient(JeapOAuth2RestClientBuilderFactory restClientBuilderFactory,
                                                    DocServiceWebhookProperties properties) {
        return new DocServiceWebhookClient(restClientBuilderFactory, properties);
    }

    /**
     * One thread of its own, so that the call neither holds the scoring's lock nor delays its completion.
     */
    @Bean
    ThreadPoolTaskExecutor docServiceWebhookExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("doc-service-webhook-");
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(10);
        return executor;
    }

    @Bean
    ScoringRunCompletedWebhookListener scoringRunCompletedWebhookListener(DocServiceWebhookClient client,
                                                                          ThreadPoolTaskExecutor docServiceWebhookExecutor) {
        return new ScoringRunCompletedWebhookListener(client, docServiceWebhookExecutor);
    }
}
