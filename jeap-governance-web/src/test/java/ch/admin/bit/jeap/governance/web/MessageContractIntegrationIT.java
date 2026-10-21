package ch.admin.bit.jeap.governance.web;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.plugin.datasource.DataSourceUnavailableException;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleParameters;
import ch.admin.bit.jeap.governance.domain.rule.State;
import ch.admin.bit.jeap.governance.messagecontract.dataimport.MessageContractVersionImporter;
import ch.admin.bit.jeap.governance.messagecontract.domain.MessageContractVersionStatusRepository;
import ch.admin.bit.jeap.governance.messagecontract.rule.ComponentUsesLatestMessageVersionsRule;
import com.github.tomakehurst.wiremock.client.BasicCredentials;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "jeap.governance.message-contract.enabled=true")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MessageContractIntegrationIT extends GovernanceIntegrationTestBase {

    @Autowired
    private MessageContractVersionImporter importer;
    @Autowired
    private MessageContractVersionStatusRepository repository;
    @Autowired
    private ComponentUsesLatestMessageVersionsRule rule;

    @Test
    void importsAuthenticatedSnapshotAndEvaluatesRule() {
        messageContractMockServer.stubFor(get(urlEqualTo("/api/contracts/version-status?env=PROD"))
                .withBasicAuth(MC_USERNAME, MC_PASSWORD)
                .willReturn(okJson("""
                        [{"appName":"orders-service","appVersion":"2.1.0","messageType":"OrderCreatedEvent",
                        "usedVersion":"1.0.0","latestVersion":"1.1.0","topic":"orders","role":"PRODUCER",
                        "upToDate":false}]
                        """)));

        importer.importData();

        var component = SystemComponent.builder()
                .name("orders-service")
                .type(ComponentType.BACKEND_SERVICE)
                .build();
        assertThat(repository.findNotUpToDateByAppName("orders-service")).hasSize(1);
        assertThat(rule.evaluate(component, new RuleParameters(Map.of())).state()).isEqualTo(State.FAIL);
        messageContractMockServer.verify(getRequestedFor(urlEqualTo("/api/contracts/version-status?env=PROD"))
                .withBasicAuth(new BasicCredentials(MC_USERNAME, MC_PASSWORD)));

        messageContractMockServer.stubFor(get(urlEqualTo("/api/contracts/version-status?env=PROD"))
                .withBasicAuth(MC_USERNAME, MC_PASSWORD)
                .willReturn(okJson("[]")));

        importer.importData();

        assertThat(repository.findNotUpToDateByAppName("orders-service")).isEmpty();
        assertThat(rule.evaluate(component, new RuleParameters(Map.of())).state()).isEqualTo(State.OK);
    }

    @Test
    void importsPartialSnapshotWithUnknownContract() {
        messageContractMockServer.stubFor(get(urlEqualTo("/api/contracts/version-status?env=PROD"))
                .withBasicAuth(MC_USERNAME, MC_PASSWORD)
                .willReturn(okJson("""
                        [{"appName":"billing-service","appVersion":"3.0.0","messageType":"SharedArtifactEvent",
                        "usedVersion":"1.0.0","latestVersion":null,"topic":"shared","role":"CONSUMER",
                        "upToDate":false,"status":"UNKNOWN"},
                        {"appName":"orders-service","appVersion":"2.1.0","messageType":"OrderCreatedEvent",
                        "usedVersion":"1.0.0","latestVersion":"1.1.0","topic":"orders","role":"PRODUCER",
                        "upToDate":false,"status":"OUTDATED"}]
                        """)));

        importer.importData();

        assertThat(rule.evaluate(backendService("orders-service"), new RuleParameters(Map.of())).state())
                .isEqualTo(State.FAIL);
        var billingResult = rule.evaluate(backendService("billing-service"), new RuleParameters(Map.of()));
        assertThat(billingResult.state()).isEqualTo(State.OK);
        assertThat(billingResult.stateComment()).contains("SharedArtifactEvent (CONSUMER on shared) uses 1.0.0");
    }

    @Test
    void retainsPreviousSnapshotWhenServiceIsUnavailable() {
        messageContractMockServer.stubFor(get(urlEqualTo("/api/contracts/version-status?env=PROD"))
                .withBasicAuth(MC_USERNAME, MC_PASSWORD)
                .willReturn(okJson("""
                        [{"appName":"orders-service","appVersion":"2.1.0","messageType":"OrderCreatedEvent",
                        "usedVersion":"1.0.0","latestVersion":"1.1.0","topic":"orders","role":"PRODUCER",
                        "upToDate":false,"status":"OUTDATED"}]
                        """)));
        importer.importData();
        messageContractMockServer.stubFor(get(urlEqualTo("/api/contracts/version-status?env=PROD"))
                .withBasicAuth(MC_USERNAME, MC_PASSWORD)
                .willReturn(aResponse().withStatus(503).withBody("retry later")));

        assertThatThrownBy(importer::importData).isInstanceOf(DataSourceUnavailableException.class);

        assertThat(repository.findNotUpToDateByAppName("orders-service")).hasSize(1);
    }

    private static SystemComponent backendService(String name) {
        return SystemComponent.builder().name(name).type(ComponentType.BACKEND_SERVICE).build();
    }
}
