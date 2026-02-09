package ch.admin.bit.jeap.governance.deploymentlog;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;

@AutoConfiguration
@PropertySource("classpath:deploymentLogDataImportDefaultProperties.properties")
@ComponentScan(basePackages = "ch.admin.bit.jeap.governance.deploymentlog")
@ConditionalOnProperty(name = "jeap.governance.deploymentlog.enabled", havingValue = "true", matchIfMissing = true)
public class DeploymentLogConfiguration {
}
