package ch.admin.bit.jeap.governance.messagecontract;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@ComponentScan
@EnableConfigurationProperties(MessageContractProperties.class)
@ConditionalOnProperty(name = "jeap.governance.message-contract.enabled", havingValue = "true")
public class MessageContractConfiguration {
}
