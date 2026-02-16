package ch.admin.bit.jeap.governance.secscan;

import ch.admin.bit.jeap.governance.secscan.dataimport.DataimportConfigurationProperties;
import ch.admin.bit.jeap.governance.secscan.domain.SecscanState;
import ch.admin.bit.jeap.governance.secscan.apidiscovery.ApiDiscoveryClientConfigurationProperties;
import ch.admin.bit.jeap.governance.secscan.httpcheck.HttpCheckConfigurationProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@ComponentScan
@EntityScan(basePackageClasses = SecscanState.class)
@EnableConfigurationProperties({ApiDiscoveryClientConfigurationProperties.class, DataimportConfigurationProperties.class,
                                HttpCheckConfigurationProperties.class})
@ConditionalOnProperty(name = "jeap.governance.secscan.enabled", havingValue = "true", matchIfMissing = true)
public class SecscanAutoconfiguration {
}
