package ch.admin.bit.jeap.governance.secscan.dataimport;

import ch.admin.bit.jeap.governance.domain.GovernanceServiceEnvironment;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "jeap.governance.secscan.dataimport", ignoreUnknownFields = false)
public class DataimportConfigurationProperties {

    /**
     * The environment to target with the security scan.
     */
    private GovernanceServiceEnvironment targetEnvironment = GovernanceServiceEnvironment.REF;

}
