package ch.admin.bit.jeap.governance.prometheus;


import ch.admin.bit.jeap.governance.prometheus.amp.AmazonManagedPromClientProperties;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeries;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;


@AutoConfiguration
@ComponentScan
@EntityScan(basePackageClasses = PromTimeSeries.class)
@EnableConfigurationProperties(AmazonManagedPromClientProperties.class)
@ConditionalOnProperty(name = "jeap.governance.prometheus.enabled", havingValue = "true", matchIfMissing = true)
public class PrometheusAutoconfiguration {
}
