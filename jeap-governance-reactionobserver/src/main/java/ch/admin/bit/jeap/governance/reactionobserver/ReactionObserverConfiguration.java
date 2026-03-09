package ch.admin.bit.jeap.governance.reactionobserver;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;

@AutoConfiguration
@PropertySource("classpath:reactionObserverDataImportDefaultProperties.properties")
@ComponentScan(basePackages = "ch.admin.bit.jeap.governance.reactionobserver")
@ConditionalOnProperty(name = "jeap.governance.reactionobserver.enabled", havingValue = "true", matchIfMissing = true)
public class ReactionObserverConfiguration {
}
