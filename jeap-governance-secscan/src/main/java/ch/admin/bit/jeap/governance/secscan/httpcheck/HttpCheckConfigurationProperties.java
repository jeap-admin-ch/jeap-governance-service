package ch.admin.bit.jeap.governance.secscan.httpcheck;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "jeap.governance.secscan.httpcheck", ignoreUnknownFields = false)
public class HttpCheckConfigurationProperties {

    /**
     * Connect timeout for an HTTP security check request. Default is 5 seconds.
     */
    private Duration connectTimeout = Duration.ofSeconds(5);

    /**
     * Read timeout for an HTTP security check request. Default is 5 seconds.
     */
    private Duration readTimeout = Duration.ofSeconds(10);

}
