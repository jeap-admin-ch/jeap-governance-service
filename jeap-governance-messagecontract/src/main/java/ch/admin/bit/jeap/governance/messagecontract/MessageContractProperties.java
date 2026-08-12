package ch.admin.bit.jeap.governance.messagecontract;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "jeap.governance.message-contract")
public class MessageContractProperties {
    private String url;
    private String environment = "PROD";
    private String username;
    private String password;
    private Duration timeout = Duration.ofSeconds(10);

    @PostConstruct
    void validate() {
        if (!StringUtils.hasText(url) || !StringUtils.hasText(environment)
                || !StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new IllegalArgumentException(
                    "MessageContractProperties 'url', 'environment', 'username' and 'password' must be provided");
        }
    }
}
