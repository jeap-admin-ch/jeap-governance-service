package ch.admin.bit.jeap.governance.web;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * {@code @EnableAsync}: the management API runs its jobs in the background, which must not depend on another starter
 * happening to enable it.
 */
@AutoConfiguration
@EnableConfigurationProperties
@EnableScheduling
@EnableAsync
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
@ComponentScan(basePackages = "ch.admin.bit.jeap.governance.web")
@PropertySource("classpath:governanceDefaultProperties.properties")
public class GovernanceWebConfig {
}
