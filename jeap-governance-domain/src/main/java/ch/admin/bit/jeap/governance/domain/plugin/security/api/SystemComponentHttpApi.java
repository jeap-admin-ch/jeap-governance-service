package ch.admin.bit.jeap.governance.domain.plugin.security.api;

import ch.admin.bit.jeap.governance.domain.GovernanceServiceEnvironment;

import java.time.ZonedDateTime;

public record SystemComponentHttpApi(
        String systemComponentName,
        GovernanceServiceEnvironment environment,
        HttpApi httpApi,
        ZonedDateTime lastUpdated) {
}
