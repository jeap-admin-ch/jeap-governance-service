package ch.admin.bit.jeap.governance.secscan.domain;

import java.util.List;

public record HttpApi(String url, String version, List<HttpEndpoint> endpoints) {
}
