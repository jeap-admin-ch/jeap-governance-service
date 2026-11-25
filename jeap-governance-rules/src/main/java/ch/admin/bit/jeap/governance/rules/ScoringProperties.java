package ch.admin.bit.jeap.governance.rules;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * The lock of the scoring job, also used when the scoring is started through the management API. The scheduled run
 * reads the same properties in its {@code @SchedulerLock}.
 */
@Data
@ConfigurationProperties(prefix = "jeap.governance.scoring")
public class ScoringProperties {
    private Duration lockAtLeast;
    private Duration lockAtMost;
}
