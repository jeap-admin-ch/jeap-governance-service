package ch.admin.bit.jeap.governance.web;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
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
        assertThat(repository.findOutdatedByAppName("orders-service")).hasSize(1);
        assertThat(rule.evaluate(component, new RuleParameters(Map.of())).state()).isEqualTo(State.FAIL);
        messageContractMockServer.verify(getRequestedFor(urlEqualTo("/api/contracts/version-status?env=PROD"))
                .withBasicAuth(new BasicCredentials(MC_USERNAME, MC_PASSWORD)));

        messageContractMockServer.stubFor(get(urlEqualTo("/api/contracts/version-status?env=PROD"))
                .withBasicAuth(MC_USERNAME, MC_PASSWORD)
                .willReturn(okJson("[]")));

        importer.importData();

        assertThat(repository.findOutdatedByAppName("orders-service")).isEmpty();
        assertThat(rule.evaluate(component, new RuleParameters(Map.of())).state()).isEqualTo(State.OK);
    }
}
