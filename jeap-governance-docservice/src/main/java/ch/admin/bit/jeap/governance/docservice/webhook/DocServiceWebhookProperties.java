package ch.admin.bit.jeap.governance.docservice.webhook;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * The webhook of the jEAP Doc Service, called after every completed scoring run so that the Doc Service imports the
 * docs API right away.
 */
@Data
@ConfigurationProperties(prefix = "jeap.governance.doc-service.webhook")
public class DocServiceWebhookProperties {

    /**
     * Calls the Doc Service's webhook after every completed scoring run. Requires the webhook URL and client
     * registration.
     */
    private boolean enabled = false;

    /**
     * The URL of the Doc Service's governance webhook, e.g. https://doc.example.com/doc-service/api/governance/webhook.
     */
    private String url;

    /**
     * The Spring Security OAuth2 client registration the webhook is called with. Its token must carry the role
     * governance-notifications / write of the Doc Service.
     */
    private String clientRegistration;

    /**
     * Connect and read timeout of the call.
     */
    private Duration timeout = Duration.ofSeconds(10);
}
