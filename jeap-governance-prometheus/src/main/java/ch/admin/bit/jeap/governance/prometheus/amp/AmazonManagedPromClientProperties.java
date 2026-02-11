package ch.admin.bit.jeap.governance.prometheus.amp;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Slf4j
@Data
@ConfigurationProperties(prefix = "jeap.governance.prometheus.amp", ignoreUnknownFields = false)
public class AmazonManagedPromClientProperties {

    private String host;

    private String roleArn;

    private String roleSessionName;

    private String workspace;

    @PostConstruct
    void init() {
        log.info("Amazon managed Prometheus client configuration: {}", this);
    }

}
