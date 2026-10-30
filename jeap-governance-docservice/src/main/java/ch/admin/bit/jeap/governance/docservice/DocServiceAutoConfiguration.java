package ch.admin.bit.jeap.governance.docservice;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * The connection to the jEAP Doc Service: the webhook call after every completed scoring run, present only while
 * {@code jeap.governance.doc-service.webhook.enabled} is on. The docs API the Doc Service reads is served by
 * {@code jeap-governance-web}.
 */
@AutoConfiguration
@ComponentScan
public class DocServiceAutoConfiguration {
}
