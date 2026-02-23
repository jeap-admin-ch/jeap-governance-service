package ch.admin.bit.jeap.governance.secscan.dataimport;

import ch.admin.bit.jeap.governance.domain.GovernanceServiceEnvironment;
import ch.admin.bit.jeap.governance.domain.SystemComponentReference;
import ch.admin.bit.jeap.governance.domain.SystemComponentRepository;
import ch.admin.bit.jeap.governance.secscan.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecscanTest {

    private static final String COMPONENT_A = "testa-backend-svc";
    private static final String COMPONENT_B = "testb-api-svc";
    private static final long COMPONENT_A_ID = COMPONENT_A.hashCode();
    private static final long COMPONENT_B_ID = COMPONENT_B.hashCode();
    private static final GovernanceServiceEnvironment ENV = GovernanceServiceEnvironment.REF;

    @Mock
    private DataimportConfigurationProperties configProperties;
    @Mock
    private SystemComponentRepository systemComponentRepository;
    @Mock
    private SystemComponentHttpApiDiscoveryClient apiDiscoveryClient;
    @Mock
    private SystemComponentHttpApiIgnoreFilter apiFilter;
    @Mock
    private HttpEndpointSecurityChecker endpointSecurityChecker;
    @Mock
    private SecscanTransactions secscanTransactions;
    @Mock
    private SecscanFlaggedEndpointRepository flaggedEndpointRepository;
    @Mock
    private SecscanStateRepository secscanStateRepository;

    @InjectMocks
    private Secscan secscan;

    @Captor
    private ArgumentCaptor<SecscanState> stateCaptor;

    @Captor
    private ArgumentCaptor<List<SecscanFlaggedEndpoint>> flaggedEndpointsCaptor;

    @BeforeEach
    void setUp() {
        when(configProperties.getTargetEnvironment()).thenReturn(ENV);
    }

    @Test
    void importData_noApiFound_deletesOldDataAndStoresNoApiState() {
        mockExecuteInTransactions();
        when(systemComponentRepository.findAllSystemComponentReferences()).thenReturn(List.of(componentReference(COMPONENT_A)));
        when(apiDiscoveryClient.discover(COMPONENT_A, ENV)).thenReturn(null);

        secscan.importData();

        verify(secscanStateRepository).deleteBySystemComponentId(COMPONENT_A_ID);
        verify(secscanStateRepository).save(stateCaptor.capture());
        SecscanState savedState = stateCaptor.getValue();
        assertThat(savedState.getSystemComponentId()).isEqualTo(COMPONENT_A_ID);
        assertThat(savedState.getScanMessage()).contains("No HTTP API found");
        assertThat(savedState.getScanTimestamp()).isNotNull();

        verify(flaggedEndpointRepository).deleteBySystemComponentId(COMPONENT_A_ID);
        verify(flaggedEndpointRepository, never()).saveAll(any());
    }

    @Test
    void importData_apiWithNullHttpApi_deletesOldDataAndStoresNoApiState() {
        mockExecuteInTransactions();
        when(systemComponentRepository.findAllSystemComponentReferences()).thenReturn(List.of(componentReference(COMPONENT_A)));
        when(apiDiscoveryClient.discover(COMPONENT_A, ENV)).thenReturn(
                new SystemComponentHttpApi(COMPONENT_A, ENV, null, null));

        secscan.importData();

        verify(secscanStateRepository).deleteBySystemComponentId(COMPONENT_A_ID);
        verify(secscanStateRepository).save(stateCaptor.capture());
        assertThat(stateCaptor.getValue().getSystemComponentId()).isEqualTo(COMPONENT_A_ID);
        assertThat(stateCaptor.getValue().getScanMessage()).contains("No HTTP API found");
    }

    @Test
    void importData_apiWithNoEndpoints_deletesOldDataAndStoresEmptyApiState() {
        mockExecuteInTransactions();
        when(systemComponentRepository.findAllSystemComponentReferences()).thenReturn(List.of(componentReference(COMPONENT_A)));
        when(apiDiscoveryClient.discover(COMPONENT_A, ENV)).thenReturn(
                createSystemComponentHttpApi(COMPONENT_A, List.of(), null));

        secscan.importData();

        verify(secscanStateRepository).deleteBySystemComponentId(COMPONENT_A_ID);
        verify(secscanStateRepository).save(stateCaptor.capture());
        assertThat(stateCaptor.getValue().getSystemComponentId()).isEqualTo(COMPONENT_A_ID);
        assertThat(stateCaptor.getValue().getScanMessage()).contains("No HTTP endpoints found");

        verify(flaggedEndpointRepository).deleteBySystemComponentId(COMPONENT_A_ID);
        verify(flaggedEndpointRepository, never()).saveAll(any());
    }

    @Test
    void importData_apiWithNullEndpoints_deletesOldDataAndStoresEmptyApiState() {
        mockExecuteInTransactions();
        when(systemComponentRepository.findAllSystemComponentReferences()).thenReturn(List.of(componentReference(COMPONENT_A)));
        when(apiDiscoveryClient.discover(COMPONENT_A, ENV)).thenReturn(
                new SystemComponentHttpApi(COMPONENT_A, ENV, new HttpApi("http://example.com", "1.0", null), null));

        secscan.importData();

        verify(secscanStateRepository).save(stateCaptor.capture());
        assertThat(stateCaptor.getValue().getScanMessage()).contains("No HTTP endpoints found");
    }

    @Test
    void importData_apiFilteredOut_deletesOldDataAndStoresIgnoredState() {
        mockExecuteInTransactions();
        when(systemComponentRepository.findAllSystemComponentReferences()).thenReturn(List.of(componentReference(COMPONENT_A)));
        when(apiDiscoveryClient.discover(COMPONENT_A, ENV)).thenReturn(
                createSystemComponentHttpApi(COMPONENT_A, List.of(new HttpEndpoint("/api/test", "GET")), null));
        when(apiFilter.shouldIgnoreApi(any())).thenReturn(
                SystemComponentHttpApiIgnoreFilter.Result.ignoredWithReason("Not in scope"));

        secscan.importData();

        verify(secscanStateRepository).deleteBySystemComponentId(COMPONENT_A_ID);
        verify(secscanStateRepository).save(stateCaptor.capture());
        assertThat(stateCaptor.getValue().getSystemComponentId()).isEqualTo(COMPONENT_A_ID);
        assertThat(stateCaptor.getValue().getScanMessage()).contains("Ignoring");
        assertThat(stateCaptor.getValue().getScanMessage()).contains("Not in scope");

        verify(flaggedEndpointRepository).deleteBySystemComponentId(COMPONENT_A_ID);
        verify(endpointSecurityChecker, never()).check(any(), any());
    }

    @Test
    void importData_scanDataStillUpToDate_doesNotUpdateDatabase() {
        mockExecuteInTransactions();
        when(systemComponentRepository.findAllSystemComponentReferences()).thenReturn(List.of(componentReference(COMPONENT_A)));
        ZonedDateTime apiLastUpdated = ZonedDateTime.now().minusHours(2);
        ZonedDateTime lastScanTimestamp = ZonedDateTime.now().minusHours(1);
        when(apiDiscoveryClient.discover(COMPONENT_A, ENV)).thenReturn(
                createSystemComponentHttpApi(COMPONENT_A, List.of(new HttpEndpoint("/api/test", "GET")), apiLastUpdated));
        when(apiFilter.shouldIgnoreApi(any())).thenReturn(SystemComponentHttpApiIgnoreFilter.Result.notIgnored());
        SecscanState existingState = SecscanState.builder()
                .systemComponentId(COMPONENT_A_ID)
                .scanMessage("previous scan")
                .scanTimestamp(lastScanTimestamp)
                .build();
        when(secscanStateRepository.findBySystemComponentId(COMPONENT_A_ID)).thenReturn(Optional.of(existingState));

        secscan.importData();

        verify(secscanStateRepository, never()).deleteBySystemComponentId(anyLong());
        verify(secscanStateRepository, never()).save(any());
        verify(flaggedEndpointRepository, never()).deleteBySystemComponentId(anyLong());
        verify(endpointSecurityChecker, never()).check(any(), any());
    }

    @Test
    void importData_scanDataOutdated_performsScan() {
        mockExecuteInTransactions();
        when(systemComponentRepository.findAllSystemComponentReferences()).thenReturn(List.of(componentReference(COMPONENT_A)));
        ZonedDateTime apiLastUpdated = ZonedDateTime.now().minusHours(1);
        ZonedDateTime lastScanTimestamp = ZonedDateTime.now().minusHours(2);
        HttpEndpoint endpoint = new HttpEndpoint("/api/test", "GET");
        when(apiDiscoveryClient.discover(COMPONENT_A, ENV)).thenReturn(
                createSystemComponentHttpApi(COMPONENT_A, List.of(endpoint), apiLastUpdated));
        when(apiFilter.shouldIgnoreApi(any())).thenReturn(SystemComponentHttpApiIgnoreFilter.Result.notIgnored());
        SecscanState existingState = SecscanState.builder()
                .systemComponentId(COMPONENT_A_ID)
                .scanMessage("previous scan")
                .scanTimestamp(lastScanTimestamp)
                .build();
        when(secscanStateRepository.findBySystemComponentId(COMPONENT_A_ID)).thenReturn(Optional.of(existingState));
        when(apiFilter.shouldIgnoreEndpoint(eq(COMPONENT_A), any())).thenReturn(SystemComponentHttpApiIgnoreFilter.Result.notIgnored());
        when(endpointSecurityChecker.check(any(), any())).thenReturn(new HttpEndpointSecurityChecker.Result(false, "passed"));

        secscan.importData();

        verify(endpointSecurityChecker).check("http://example.com", endpoint);
        verify(secscanStateRepository).save(any());
    }

    @Test
    void importData_lastUpdatedPresent_noPreviousScanState_performsScan() {
        mockExecuteInTransactions();
        when(systemComponentRepository.findAllSystemComponentReferences()).thenReturn(List.of(componentReference(COMPONENT_A)));
        ZonedDateTime apiLastUpdated = ZonedDateTime.now().minusHours(1);
        HttpEndpoint endpoint = new HttpEndpoint("/api/test", "GET");
        when(apiDiscoveryClient.discover(COMPONENT_A, ENV)).thenReturn(
                createSystemComponentHttpApi(COMPONENT_A, List.of(endpoint), apiLastUpdated));
        when(apiFilter.shouldIgnoreApi(any())).thenReturn(SystemComponentHttpApiIgnoreFilter.Result.notIgnored());
        when(secscanStateRepository.findBySystemComponentId(COMPONENT_A_ID)).thenReturn(Optional.empty());
        when(apiFilter.shouldIgnoreEndpoint(eq(COMPONENT_A), any())).thenReturn(SystemComponentHttpApiIgnoreFilter.Result.notIgnored());
        when(endpointSecurityChecker.check(any(), any())).thenReturn(new HttpEndpointSecurityChecker.Result(false, "passed"));

        secscan.importData();

        verify(endpointSecurityChecker).check("http://example.com", endpoint);
        verify(secscanStateRepository).save(any());
    }

    @Test
    void importData_noLastUpdated_alwaysScans() {
        mockExecuteInTransactions();
        when(systemComponentRepository.findAllSystemComponentReferences()).thenReturn(List.of(componentReference(COMPONENT_A)));
        HttpEndpoint endpoint = new HttpEndpoint("/api/test", "GET");
        when(apiDiscoveryClient.discover(COMPONENT_A, ENV)).thenReturn(
                createSystemComponentHttpApi(COMPONENT_A, List.of(endpoint), null));
        when(apiFilter.shouldIgnoreApi(any())).thenReturn(SystemComponentHttpApiIgnoreFilter.Result.notIgnored());
        when(apiFilter.shouldIgnoreEndpoint(eq(COMPONENT_A), any())).thenReturn(SystemComponentHttpApiIgnoreFilter.Result.notIgnored());
        when(endpointSecurityChecker.check(any(), any())).thenReturn(new HttpEndpointSecurityChecker.Result(false, "passed"));

        secscan.importData();

        verify(endpointSecurityChecker).check("http://example.com", endpoint);
        verify(secscanStateRepository, never()).findBySystemComponentId(anyLong());
    }

    @Test
    void importData_allEndpointsSecured_storesOkStateAndNoFlaggedEndpoints() {
        mockExecuteInTransactions();
        when(systemComponentRepository.findAllSystemComponentReferences()).thenReturn(List.of(componentReference(COMPONENT_A)));
        HttpEndpoint endpoint1 = new HttpEndpoint("/api/users", "GET");
        HttpEndpoint endpoint2 = new HttpEndpoint("/api/orders", "GET");
        when(apiDiscoveryClient.discover(COMPONENT_A, ENV)).thenReturn(
                createSystemComponentHttpApi(COMPONENT_A, List.of(endpoint1, endpoint2), null));
        when(apiFilter.shouldIgnoreApi(any())).thenReturn(SystemComponentHttpApiIgnoreFilter.Result.notIgnored());
        when(apiFilter.shouldIgnoreEndpoint(eq(COMPONENT_A), any())).thenReturn(SystemComponentHttpApiIgnoreFilter.Result.notIgnored());
        when(endpointSecurityChecker.check(any(), any())).thenReturn(
                new HttpEndpointSecurityChecker.Result(false, "Endpoint passed check by returning status 401 UNAUTHORIZED."));

        secscan.importData();

        verify(secscanStateRepository).deleteBySystemComponentId(COMPONENT_A_ID);
        verify(secscanStateRepository).save(stateCaptor.capture());
        assertThat(stateCaptor.getValue().getSystemComponentId()).isEqualTo(COMPONENT_A_ID);
        assertThat(stateCaptor.getValue().getScanMessage()).contains("did not flag any endpoint");

        verify(flaggedEndpointRepository).deleteBySystemComponentId(COMPONENT_A_ID);
        verify(flaggedEndpointRepository, never()).saveAll(any());
    }

    @Test
    void importData_someEndpointsFlagged_storesFlaggedStateAndFlaggedEndpoints() {
        mockExecuteInTransactions();
        when(systemComponentRepository.findAllSystemComponentReferences()).thenReturn(List.of(componentReference(COMPONENT_A)));
        HttpEndpoint securedEndpoint = new HttpEndpoint("/api/secured", "GET");
        HttpEndpoint unsecuredEndpoint = new HttpEndpoint("/api/open", "GET");
        when(apiDiscoveryClient.discover(COMPONENT_A, ENV)).thenReturn(
                createSystemComponentHttpApi(COMPONENT_A, List.of(securedEndpoint, unsecuredEndpoint), null));
        when(apiFilter.shouldIgnoreApi(any())).thenReturn(SystemComponentHttpApiIgnoreFilter.Result.notIgnored());
        when(apiFilter.shouldIgnoreEndpoint(eq(COMPONENT_A), any())).thenReturn(SystemComponentHttpApiIgnoreFilter.Result.notIgnored());
        when(endpointSecurityChecker.check("http://example.com", securedEndpoint))
                .thenReturn(new HttpEndpointSecurityChecker.Result(false, "passed"));
        when(endpointSecurityChecker.check("http://example.com", unsecuredEndpoint))
                .thenReturn(new HttpEndpointSecurityChecker.Result(true, "Endpoint failed check by returning status 200 OK."));

        secscan.importData();

        verify(secscanStateRepository).deleteBySystemComponentId(COMPONENT_A_ID);
        verify(secscanStateRepository).save(stateCaptor.capture());
        assertThat(stateCaptor.getValue().getSystemComponentId()).isEqualTo(COMPONENT_A_ID);
        assertThat(stateCaptor.getValue().getScanMessage()).contains("flagged 1 endpoints");

        verify(flaggedEndpointRepository).deleteBySystemComponentId(COMPONENT_A_ID);
        verify(flaggedEndpointRepository).saveAll(flaggedEndpointsCaptor.capture());
        List<SecscanFlaggedEndpoint> flaggedEndpoints = flaggedEndpointsCaptor.getValue();
        assertThat(flaggedEndpoints).hasSize(1);
        assertThat(flaggedEndpoints.getFirst().getSystemComponentId()).isEqualTo(COMPONENT_A_ID);
        assertThat(flaggedEndpoints.getFirst().getPath()).isEqualTo("/api/open");
        assertThat(flaggedEndpoints.getFirst().getMethod()).isEqualTo("GET");
        assertThat(flaggedEndpoints.getFirst().getScanMessage()).contains("Endpoint failed check");
    }

    @Test
    void importData_endpointFilteredOut_skipsFilteredEndpoint() {
        mockExecuteInTransactions();
        when(systemComponentRepository.findAllSystemComponentReferences()).thenReturn(List.of(componentReference(COMPONENT_A)));
        HttpEndpoint getEndpoint = new HttpEndpoint("/api/users", "GET");
        HttpEndpoint postEndpoint = new HttpEndpoint("/api/users", "POST");
        when(apiDiscoveryClient.discover(COMPONENT_A, ENV)).thenReturn(
                createSystemComponentHttpApi(COMPONENT_A, List.of(getEndpoint, postEndpoint), null));
        when(apiFilter.shouldIgnoreApi(any())).thenReturn(SystemComponentHttpApiIgnoreFilter.Result.notIgnored());
        when(apiFilter.shouldIgnoreEndpoint(COMPONENT_A, getEndpoint)).thenReturn(SystemComponentHttpApiIgnoreFilter.Result.notIgnored());
        when(apiFilter.shouldIgnoreEndpoint(COMPONENT_A, postEndpoint)).thenReturn(
                SystemComponentHttpApiIgnoreFilter.Result.ignoredWithReason("Only GET requests"));
        when(endpointSecurityChecker.check("http://example.com", getEndpoint))
                .thenReturn(new HttpEndpointSecurityChecker.Result(false, "passed"));

        secscan.importData();

        verify(endpointSecurityChecker).check("http://example.com", getEndpoint);
        verify(endpointSecurityChecker, never()).check(any(), eq(postEndpoint));
    }

    @Test
    void importData_allEndpointsFilteredOut_storesOkStateAndNoFlaggedEndpoints() {
        mockExecuteInTransactions();
        when(systemComponentRepository.findAllSystemComponentReferences()).thenReturn(List.of(componentReference(COMPONENT_A)));
        HttpEndpoint postEndpoint = new HttpEndpoint("/api/users", "POST");
        HttpEndpoint putEndpoint = new HttpEndpoint("/api/users", "PUT");
        when(apiDiscoveryClient.discover(COMPONENT_A, ENV)).thenReturn(
                createSystemComponentHttpApi(COMPONENT_A, List.of(postEndpoint, putEndpoint), null));
        when(apiFilter.shouldIgnoreApi(any())).thenReturn(SystemComponentHttpApiIgnoreFilter.Result.notIgnored());
        when(apiFilter.shouldIgnoreEndpoint(eq(COMPONENT_A), any())).thenReturn(
                SystemComponentHttpApiIgnoreFilter.Result.ignoredWithReason("Only GET requests"));

        secscan.importData();

        verify(endpointSecurityChecker, never()).check(any(), any());

        verify(secscanStateRepository).deleteBySystemComponentId(COMPONENT_A_ID);
        verify(secscanStateRepository).save(stateCaptor.capture());
        assertThat(stateCaptor.getValue().getSystemComponentId()).isEqualTo(COMPONENT_A_ID);
        assertThat(stateCaptor.getValue().getScanMessage()).contains("did not flag any endpoint");

        verify(flaggedEndpointRepository).deleteBySystemComponentId(COMPONENT_A_ID);
        verify(flaggedEndpointRepository, never()).saveAll(any());
    }

    @Test
    void importData_securityCheckThrowsException_endpointGivenBenefitOfDoubt() {
        mockExecuteInTransactions();
        when(systemComponentRepository.findAllSystemComponentReferences()).thenReturn(List.of(componentReference(COMPONENT_A)));
        HttpEndpoint endpoint = new HttpEndpoint("/api/unreachable", "GET");
        when(apiDiscoveryClient.discover(COMPONENT_A, ENV)).thenReturn(
                createSystemComponentHttpApi(COMPONENT_A, List.of(endpoint), null));
        when(apiFilter.shouldIgnoreApi(any())).thenReturn(SystemComponentHttpApiIgnoreFilter.Result.notIgnored());
        when(apiFilter.shouldIgnoreEndpoint(eq(COMPONENT_A), any())).thenReturn(SystemComponentHttpApiIgnoreFilter.Result.notIgnored());
        when(endpointSecurityChecker.check(any(), any())).thenThrow(new RuntimeException("Connection refused"));

        secscan.importData();

        verify(secscanStateRepository).save(stateCaptor.capture());
        assertThat(stateCaptor.getValue().getScanMessage()).contains("did not flag any endpoint");

        verify(flaggedEndpointRepository).deleteBySystemComponentId(COMPONENT_A_ID);
        verify(flaggedEndpointRepository, never()).saveAll(any());
    }

    @Test
    void importData_discoveryThrowsException_leavesExistingDataUnchanged() {

        when(systemComponentRepository.findAllSystemComponentReferences()).thenReturn(List.of(componentReference(COMPONENT_A)));
        when(apiDiscoveryClient.discover(COMPONENT_A, ENV)).thenThrow(new RuntimeException("Service unavailable"));

        secscan.importData();

        verify(secscanStateRepository, never()).deleteBySystemComponentId(anyLong());
        verify(secscanStateRepository, never()).save(any());
        verify(flaggedEndpointRepository, never()).deleteBySystemComponentId(anyLong());
        verify(flaggedEndpointRepository, never()).saveAll(any());
        verify(secscanTransactions, never()).inNewTransaction(any());
    }

    @Test
    void importData_multipleComponents_processesEachIndependently() {
        mockExecuteInTransactions();
        when(systemComponentRepository.findAllSystemComponentReferences()).thenReturn(List.of(componentReference(COMPONENT_A), componentReference(COMPONENT_B)));
        when(apiDiscoveryClient.discover(COMPONENT_A, ENV)).thenThrow(new RuntimeException("Service unavailable"));
        when(apiDiscoveryClient.discover(COMPONENT_B, ENV)).thenReturn(null);

        secscan.importData();

        // Component A failed — no data update
        verify(secscanStateRepository, never()).deleteBySystemComponentId(COMPONENT_A_ID);
        verify(secscanStateRepository, never()).save(argThat(state -> COMPONENT_A_ID == state.getSystemComponentId()));

        // Component B succeeded — data updated with NO_API result
        verify(secscanStateRepository).deleteBySystemComponentId(COMPONENT_B_ID);
        verify(secscanStateRepository).save(stateCaptor.capture());
        assertThat(stateCaptor.getValue().getSystemComponentId()).isEqualTo(COMPONENT_B_ID);
    }

    @Test
    void importData_multipleFlaggedEndpoints_allStored() {
        mockExecuteInTransactions();
        when(systemComponentRepository.findAllSystemComponentReferences()).thenReturn(List.of(componentReference(COMPONENT_A)));
        HttpEndpoint ep1 = new HttpEndpoint("/api/users", "GET");
        HttpEndpoint ep2 = new HttpEndpoint("/api/admin", "GET");
        HttpEndpoint ep3 = new HttpEndpoint("/api/config", "GET");
        when(apiDiscoveryClient.discover(COMPONENT_A, ENV)).thenReturn(
                createSystemComponentHttpApi(COMPONENT_A, List.of(ep1, ep2, ep3), null));
        when(apiFilter.shouldIgnoreApi(any())).thenReturn(SystemComponentHttpApiIgnoreFilter.Result.notIgnored());
        when(apiFilter.shouldIgnoreEndpoint(eq(COMPONENT_A), any())).thenReturn(SystemComponentHttpApiIgnoreFilter.Result.notIgnored());
        when(endpointSecurityChecker.check(any(), any()))
                .thenReturn(new HttpEndpointSecurityChecker.Result(true, "returned 200"));

        secscan.importData();

        verify(flaggedEndpointRepository).saveAll(flaggedEndpointsCaptor.capture());
        List<SecscanFlaggedEndpoint> flagged = flaggedEndpointsCaptor.getValue();
        assertThat(flagged).hasSize(3);
        assertThat(flagged).extracting(SecscanFlaggedEndpoint::getPath)
                .containsExactly("/api/users", "/api/admin", "/api/config");
        assertThat(flagged).extracting(SecscanFlaggedEndpoint::getMethod)
                .containsOnly("GET");
        assertThat(flagged).allSatisfy(ep -> {
            assertThat(ep.getSystemComponentId()).isEqualTo(COMPONENT_A_ID);
            assertThat(ep.getScanTimestamp()).isNotNull();
            assertThat(ep.getScanMessage()).isEqualTo("returned 200");
        });
    }

    private void mockExecuteInTransactions() {
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(secscanTransactions).inNewTransaction(any());
    }

    @SuppressWarnings("SameParameterValue")
    private SystemComponentHttpApi createSystemComponentHttpApi(String componentName, List<HttpEndpoint> endpoints, ZonedDateTime lastUpdated) {
        HttpApi httpApi = new HttpApi("http://example.com", "1.0", endpoints);
        return new SystemComponentHttpApi(componentName, ENV, httpApi, lastUpdated);
    }

    private static SystemComponentReference componentReference(String name) {
        return new SystemComponentReference() {
            @Override
            public long getId() {
                return name.hashCode();
            }

            @Override
            public String getName() {
                return name;
            }
        };
    }
}
