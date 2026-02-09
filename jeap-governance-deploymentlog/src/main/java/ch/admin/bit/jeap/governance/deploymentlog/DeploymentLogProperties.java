package ch.admin.bit.jeap.governance.deploymentlog;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Data
@Configuration("deploymentLogProperties")
@ConfigurationProperties(prefix = "jeap.governance.deploymentlog")
@Slf4j
public class DeploymentLogProperties {

    private String url;
    private String username;
    private String password;
    private Duration timeout;

    @PostConstruct
    void checkAndLog() {
        log.info("DeploymentLogProperties initialized with url='{}', timeout='{}'", url, timeout);
        if (!StringUtils.hasText(url) || !StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new IllegalArgumentException("DeploymentLogProperties 'url', 'username' and 'password' must be provided and not empty.");
        }
    }
}
