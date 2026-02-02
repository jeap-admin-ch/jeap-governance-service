package ch.admin.bit.jeap.governance.dataimport;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Data
@Configuration("dataImportProperties")
@ConfigurationProperties(prefix = "jeap.governance.dataimport")
@Slf4j
public class DataImportProperties {
    private String cronExpression;
    private Duration lockAtLeast;
    private Duration lockAtMost;

    @PostConstruct
    void log() {
        log.info("DataImportProperties initialized with cronExpression='{}', lockAtLeast='{}', lockAtMost='{}'",
                cronExpression, lockAtLeast, lockAtMost);
    }
}
