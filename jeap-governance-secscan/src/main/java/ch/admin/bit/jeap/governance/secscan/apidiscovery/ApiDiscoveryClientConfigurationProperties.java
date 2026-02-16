package ch.admin.bit.jeap.governance.secscan.apidiscovery;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "jeap.governance.secscan.apidiscovery", ignoreUnknownFields = false)
public class ApiDiscoveryClientConfigurationProperties {

    /**
     * URL template for the APIs discovery service. The template may contain the following placeholders:
     * <p>
     * {env} will be replaced (if present) with the environment name in lower case
     * (see enum GovernanceServiceEnvironment).
     * </p>
     * <p>
     * {systemComponentName} will be replaced (if present) with the system component name.
     * </p>
     **/
    private String urlTemplate;

    /**
     * Timeout for an API discovery request. Default is 60 seconds.
     */
    private Duration timeout = Duration.ofSeconds(60);

}
