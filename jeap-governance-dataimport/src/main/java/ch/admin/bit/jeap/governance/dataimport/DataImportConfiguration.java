package ch.admin.bit.jeap.governance.dataimport;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;

@AutoConfiguration
@PropertySource("classpath:dataImportDefaultProperties.properties")
@ComponentScan(basePackages = "ch.admin.bit.jeap.governance.dataimport")
public class DataImportConfiguration {
}
