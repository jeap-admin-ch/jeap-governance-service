package ch.admin.bit.jeap.governance.prometheus.amp;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Slf4j
@Data
@ConfigurationProperties(prefix = "jeap.governance.prometheus.amp", ignoreUnknownFields = false)
public class AmazonManagedPromClientProperties {

    private String host;

    private String roleArn;

    private String roleSessionName;

    private String workspace;

    /**
     * How far back the Prometheus queries look for the most recent sample of a service. See
     * {@link AmazonManagedPromClient} for how this window affects the governance rules based on Prometheus data.
     */
    private Duration queryLookback = Duration.ofHours(6);

    @PostConstruct
    void init() {
        log.info("Amazon managed Prometheus client configuration: {}", this);
    }

}
