package ch.admin.bit.jeap.governance.secscan.domain;

import ch.admin.bit.jeap.governance.domain.plugin.security.api.HttpEndpoint;

public interface HttpEndpointSecurityChecker {

    record Result (boolean failed, String reason) {}

    Result check(String apiUrl, HttpEndpoint endpoint);

}
