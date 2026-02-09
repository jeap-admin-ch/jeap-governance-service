package ch.admin.bit.jeap.governance.archrepo;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;

@AutoConfiguration
@PropertySource("classpath:archRepoDataImportDefaultProperties.properties")
@ComponentScan(basePackages = "ch.admin.bit.jeap.governance.archrepo")
public class ArchRepoConfiguration {
}
