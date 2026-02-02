package ch.admin.bit.jeap.governance.archrepo;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Data
@Configuration("archRepoProperties")
@ConfigurationProperties(prefix = "jeap.governance.archrepo")
@Slf4j
public class ArchRepoProperties {

    private String url;
    private Duration timeout;

    @PostConstruct
    void checkAndLog() {
        log.info("ArchRepoProperties initialized with url='{}', timeout='{}'", url, timeout);
        if (!StringUtils.hasText(url)) {
            throw new IllegalArgumentException("ArchRepoProperties 'url' must be set");
        }
    }
}
