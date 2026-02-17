package ch.admin.bit.jeap.governance.rules;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;

@AutoConfiguration
@ComponentScan
@PropertySource("classpath:scoringDefaultProperties.properties")
@EnableConfigurationProperties(RuleConfigurationProperties.class)
public class RulesAutoConfiguration {
}
