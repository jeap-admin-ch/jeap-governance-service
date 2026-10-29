package ch.admin.bit.jeap.governance.reactionobserver;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Data
@Configuration("reactionObserverProperties")
@ConfigurationProperties(prefix = "jeap.governance.reactionobserver")
@Slf4j
public class ReactionObserverProperties {

    private String url;
    private String clientRegistration;
    private Duration timeout;

    @PostConstruct
    void checkAndLog() {
        log.info("ReactionObserverProperties initialized with url='{}', timeout='{}'", url, timeout);
        if (!StringUtils.hasText(url) || !StringUtils.hasText(clientRegistration)) {
            throw new IllegalArgumentException("ReactionObserverProperties 'url' and 'clientRegistration' must be provided and not empty.");
        }
    }
}
