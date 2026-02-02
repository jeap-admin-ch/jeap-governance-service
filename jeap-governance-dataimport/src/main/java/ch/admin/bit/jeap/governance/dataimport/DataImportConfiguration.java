package ch.admin.bit.jeap.governance.dataimport;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.PropertySource;

@AutoConfiguration
@EnableConfigurationProperties(DataImportProperties.class)
@PropertySource("classpath:dataImportDefaultProperties.properties")
public class DataImportConfiguration {
}
