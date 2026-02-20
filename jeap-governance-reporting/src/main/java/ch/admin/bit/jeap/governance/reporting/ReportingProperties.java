package ch.admin.bit.jeap.governance.reporting;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Data
@Configuration("reportingProperties")
@ConfigurationProperties(prefix = "jeap.governance.reporting")
@Slf4j
public class ReportingProperties {

    private int trendPeriodDays;
    private ConfluenceProperties confluence = new ConfluenceProperties();

    @PostConstruct
    void checkAndLog() {
        log.info("ReportingProperties initialized with trendPeriodDays='{}' and confluence properties='{}'", trendPeriodDays, confluence);
        if (!StringUtils.hasText(confluence.getUrl())
                || !StringUtils.hasText(confluence.getSpaceKey())
                || !StringUtils.hasText(confluence.getRootPageName())
                || !StringUtils.hasText(confluence.getAncestorId())
                || !StringUtils.hasText(confluence.getUsername())
                || !StringUtils.hasText(confluence.getPassword())) {
            log.error("""
                    ReportingProperties: Confluence properties are not properly set. Please set
                      jeap.governance.reporting.confluence.url,
                      jeap.governance.reporting.confluence.spaceKey,
                      jeap.governance.reporting.confluence.rootPageName,
                      jeap.governance.reporting.confluence.ancestorId,
                      jeap.governance.reporting.confluence.username and
                      jeap.governance.reporting.confluence.password
                    in the configuration.
                    """);
            throw new IllegalArgumentException("ReportingProperties: Confluence properties are not properly set. Please check the configuration.");
        }
    }

    public String getConfluenceSpaceKey() {
        return confluence.getSpaceKey();
    }

    public String getConfluenceUrl() {
        return confluence.getUrl();
    }

    public String getConfluenceUsername() {
        return confluence.getUsername();
    }

    public String getConfluencePassword() {
        return confluence.getPassword();
    }

    public String getConfluenceRootPageName() {
        return confluence.getRootPageName();
    }

    public String getConfluenceAncestorId() {
        return confluence.getAncestorId();
    }

    @Data
    public static class ConfluenceProperties {
        private String url;
        private String spaceKey;
        private String rootPageName;
        private String ancestorId;
        private String username;
        @ToString.Exclude
        private String password;
    }
}
