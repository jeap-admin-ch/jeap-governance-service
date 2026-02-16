package ch.admin.bit.jeap.governance.secscan.domain;

public interface HttpEndpointSecurityChecker {

    record Result (boolean failed, String reason) {}

    Result check(String apiUrl, HttpEndpoint endpoint);

}
