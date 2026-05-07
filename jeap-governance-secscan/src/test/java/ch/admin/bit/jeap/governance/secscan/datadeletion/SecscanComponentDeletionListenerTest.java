package ch.admin.bit.jeap.governance.secscan.datadeletion;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.SystemComponentRepository;
import ch.admin.bit.jeap.governance.domain.rule.RuleRepository;
import ch.admin.bit.jeap.governance.secscan.SecscanAutoconfiguration;
import ch.admin.bit.jeap.governance.secscan.domain.HttpEndpointSecurityChecker;
import ch.admin.bit.jeap.governance.secscan.domain.SecscanFlaggedEndpoint;
import ch.admin.bit.jeap.governance.secscan.domain.SecscanState;
import ch.admin.bit.jeap.governance.secscan.domain.SystemComponentHttpApiDiscoveryClient;
import ch.admin.bit.jeap.governance.secscan.persistence.PostgresTestContainerBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(SecscanAutoconfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SecscanComponentDeletionListenerTest extends PostgresTestContainerBase {

    @MockitoBean
    private SystemComponentRepository systemComponentRepository;

    @MockitoBean
    private SystemComponentHttpApiDiscoveryClient apiDiscoveryClient;

    @MockitoBean
    private HttpEndpointSecurityChecker httpEndpointSecurityChecker;

    @MockitoBean
    private RuleRepository ruleRepository;

    @Autowired
    private SecscanComponentDeletionListener deletionListener;

    @Test
    void preComponentDeletion_deletesRelatedSecscanData() {
        SystemComponent myComponent = persistSystemComponent("mysystem-mycomp-svc");
        SystemComponent otherComponent = persistSystemComponent("othersystem-othercomp-svc");
        persistState(myComponent.getId(), "scan completed");
        persistFlaggedEndpoint(myComponent.getId(), "/api/users", "GET", "Missing auth");
        persistFlaggedEndpoint(myComponent.getId(), "/api/admin", "POST", "No authz");
        persistState(otherComponent.getId(), "scan completed");
        persistFlaggedEndpoint(otherComponent.getId(), "/api/health", "GET", "Open endpoint");
        assertThat(findAllStates()).hasSize(2);
        assertThat(findAllFlaggedEndpoints()).hasSize(3);

        deletionListener.preComponentDeletion(myComponent.getId());

        List<SecscanState> remainingStates = findAllStates();
        assertThat(remainingStates).hasSize(1);
        assertThat(remainingStates.getFirst().getSystemComponentId()).isEqualTo(otherComponent.getId());
        assertThat(remainingStates.getFirst().getScanMessage()).isEqualTo("scan completed");

        List<SecscanFlaggedEndpoint> remainingEndpoints = findAllFlaggedEndpoints();
        assertThat(remainingEndpoints).hasSize(1);
        assertThat(remainingEndpoints.getFirst().getSystemComponentId()).isEqualTo(otherComponent.getId());
        assertThat(remainingEndpoints.getFirst().getPath()).isEqualTo("/api/health");
        assertThat(remainingEndpoints.getFirst().getMethod()).isEqualTo("GET");
    }

    @Test
    void preComponentDeletion_withUnknownId_doesNothing() {
        SystemComponent myComponent = persistSystemComponent("mysystem-mycomp-svc");
        persistState(myComponent.getId(), "scan completed");
        persistFlaggedEndpoint(myComponent.getId(), "/api/users", "GET", "flagged");

        deletionListener.preComponentDeletion(999L);

        assertThat(findAllStates()).hasSize(1);
        assertThat(findAllFlaggedEndpoints()).hasSize(1);
    }

    @Test
    void preComponentDeletion_withNoMatchingData_deletesNothing() {
        SystemComponent otherComponent = persistSystemComponent("othersystem-othercomp-svc");
        SystemComponent emptyComponent = persistSystemComponent("nosystem-nocomp-svc");
        persistState(otherComponent.getId(), "scan completed");
        persistFlaggedEndpoint(otherComponent.getId(), "/api/health", "GET", "flagged");

        deletionListener.preComponentDeletion(emptyComponent.getId());

        assertThat(findAllStates()).hasSize(1);
        assertThat(findAllFlaggedEndpoints()).hasSize(1);
    }

    @SuppressWarnings("SameParameterValue")
    private void persistState(long systemComponentId, String scanMessage) {
        SecscanState state = SecscanState.builder()
                .systemComponentId(systemComponentId)
                .scanMessage(scanMessage)
                .scanTimestamp(ZonedDateTime.now())
                .build();
        entityManager.persistAndFlush(state);
        entityManager.clear();
    }

    private void persistFlaggedEndpoint(long systemComponentId, String path, String method, String scanMessage) {
        SecscanFlaggedEndpoint endpoint = SecscanFlaggedEndpoint.builder()
                .systemComponentId(systemComponentId)
                .path(path)
                .method(method)
                .scanMessage(scanMessage)
                .scanTimestamp(ZonedDateTime.now())
                .build();
        entityManager.persistAndFlush(endpoint);
        entityManager.clear();
    }

    private List<SecscanState> findAllStates() {
        return entityManager.getEntityManager()
                .createQuery("SELECT s FROM SecscanState s", SecscanState.class)
                .getResultList();
    }

    private List<SecscanFlaggedEndpoint> findAllFlaggedEndpoints() {
        return entityManager.getEntityManager()
                .createQuery("SELECT fe FROM SecscanFlaggedEndpoint fe", SecscanFlaggedEndpoint.class)
                .getResultList();
    }
}
