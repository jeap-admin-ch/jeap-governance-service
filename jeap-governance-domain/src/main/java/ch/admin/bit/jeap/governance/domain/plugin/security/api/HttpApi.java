package ch.admin.bit.jeap.governance.domain.plugin.security.api;

import java.util.List;

public record HttpApi(String url, String version, List<HttpEndpoint> endpoints) {
}
