package ch.admin.bit.jeap.governance.web.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "jeap.governance.service")
public record GovernanceProperties(
        @NotBlank String dummy) {
}
