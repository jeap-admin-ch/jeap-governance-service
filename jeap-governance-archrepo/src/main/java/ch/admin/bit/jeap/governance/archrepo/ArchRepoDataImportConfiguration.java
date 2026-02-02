package ch.admin.bit.jeap.governance.archrepo;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.PropertySource;

@AutoConfiguration
@EnableConfigurationProperties(ArchRepoProperties.class)
@PropertySource("classpath:archRepoDataImportDefaultProperties.properties")
public class ArchRepoDataImportConfiguration {
}
