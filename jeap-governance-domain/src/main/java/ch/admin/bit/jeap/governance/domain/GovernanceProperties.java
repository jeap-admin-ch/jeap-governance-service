package ch.admin.bit.jeap.governance.domain;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration("governanceProperties")
@ConfigurationProperties(prefix = "jeap.governance")
@Slf4j
public class GovernanceProperties {

    private GovernanceServiceEnvironment environment;

    @PostConstruct
    void checkAndLog() {
        log.info("DeploymentLogProperties initialized with environment:'{}'", environment);
        if (environment == null) {
            throw new IllegalArgumentException("GovernanceProperties 'environment' must be provided.");
        }
    }
}
