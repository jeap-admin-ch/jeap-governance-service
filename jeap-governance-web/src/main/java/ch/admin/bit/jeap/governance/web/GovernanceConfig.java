package ch.admin.bit.jeap.governance.web;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

@AutoConfiguration
@EnableConfigurationProperties
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
@ComponentScan(basePackages = "ch.admin.bit.jeap.governance")
@PropertySource("classpath:governanceDefaultProperties.properties")
public class GovernanceConfig {
}
