package ch.admin.bit.jeap.governance.web;

import ch.admin.bit.jeap.governance.dataimport.DataImportScheduler;
import ch.admin.bit.jeap.governance.secscan.domain.SystemComponentHttpApiIgnoreFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.ZonedDateTime;
import java.util.*;

import static ch.admin.bit.jeap.governance.web.ImportModelHelper.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "jeap.governance.secscan.enabled=true",
        "jeap.governance.secscan.dataimport.target-environment=DEV"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DataImportSecscanIT extends GovernanceIntegrationTestBase {

    private static final Set<String> REMOVED_COMPONENTS = removedComponents();

    private static Set<String> removedComponents() {
        Set<String> removed = new HashSet<>(DEFAULT_MODEL_COMPONENT_NAMES);
        removed.removeAll(LESS_MODEL_COMPONENT_NAMES);
        return Collections.unmodifiableSet(removed);
    }

    protected static WireMockServer secscanMockServer;

    @Autowired
    private DataImportScheduler dataImportScheduler;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SystemComponentHttpApiIgnoreFilter apiIgnoreFilter;

    @BeforeAll
    static void startSecscanMockServer() {
        secscanMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        secscanMockServer.start();
    }

    @AfterAll
    static void stopSecscanMockServer() {
        if (secscanMockServer != null) {
            secscanMockServer.stop();
        }
    }

    @DynamicPropertySource
    static void configureSecscanProperties(DynamicPropertyRegistry registry) {
        registry.add("jeap.governance.secscan.apidiscovery.url-template",
                () -> secscanMockServer.baseUrl() + "/api-discovery/{env}/{systemComponentName}");
    }

    @BeforeEach
    void setUpSecscan() {
        cleanSecscanTables();
        secscanMockServer.resetAll();
        when(apiIgnoreFilter.shouldIgnoreApi(any())).thenReturn(SystemComponentHttpApiIgnoreFilter.Result.notIgnored());
        when(apiIgnoreFilter.shouldIgnoreEndpoint(any(), any())).thenReturn(SystemComponentHttpApiIgnoreFilter.Result.notIgnored());
    }

    @Test
    void importData_securedEndpoints_scannedOkState() throws Exception {
        setUpImportDefaultModel();
        stubApiDiscovery(COMPONENT_A1_NAME, ZonedDateTime.now().minusDays(1),
                List.of(endpoint("GET", "/api/resource1"), endpoint("POST", "/api/resource2")));
        stubEndpointResponse("/services/" + COMPONENT_A1_NAME + "/api/resource1", "GET", 401);
        stubEndpointResponse("/services/" + COMPONENT_A1_NAME + "/api/resource2", "POST", 401);

        dataImportScheduler.update();

        List<Map<String, Object>> states = querySecscanStates(COMPONENT_A1_NAME);
        assertThat(states).hasSize(1);
        assertThat((String) states.getFirst().get("scan_message")).contains("did not flag any endpoint");

        List<Map<String, Object>> flagged = queryFlaggedEndpoints(COMPONENT_A1_NAME);
        assertThat(flagged).isEmpty();
    }

    @Test
    void importData_unsecuredEndpoints_flaggedEndpoints() throws Exception {
        setUpImportDefaultModel();
        stubApiDiscovery(COMPONENT_A1_NAME, ZonedDateTime.now().minusDays(1),
                List.of(endpoint("GET", "/api/open1"), endpoint("POST", "/api/open2"), endpoint("DELETE", "/api/secure")));
        stubEndpointResponse("/services/" + COMPONENT_A1_NAME + "/api/open1", "GET", 200);
        stubEndpointResponse("/services/" + COMPONENT_A1_NAME + "/api/open2", "POST", 200);
        stubEndpointResponse("/services/" + COMPONENT_A1_NAME + "/api/secure", "DELETE", 401);

        dataImportScheduler.update();

        List<Map<String, Object>> states = querySecscanStates(COMPONENT_A1_NAME);
        assertThat(states).hasSize(1);
        assertThat((String) states.getFirst().get("scan_message")).contains("flagged 2 endpoints");

        List<Map<String, Object>> flagged = queryFlaggedEndpoints(COMPONENT_A1_NAME);
        assertThat(flagged)
                .hasSize(2)
                .extracting(row -> row.get("method"), row -> row.get("path"))
                .containsExactlyInAnyOrder(
                        tuple("GET", "/api/open1"),
                        tuple("POST", "/api/open2"));
    }

    @Test
    void importData_noApiFound_noApiState() throws Exception {
        setUpImportDefaultModel();
        // No API discovery stub for A1 -> WireMock returns 404 by default

        dataImportScheduler.update();

        List<Map<String, Object>> states = querySecscanStates(COMPONENT_A1_NAME);
        assertThat(states).hasSize(1);
        assertThat((String) states.getFirst().get("scan_message")).contains("No HTTP API found");

        List<Map<String, Object>> flagged = queryFlaggedEndpoints(COMPONENT_A1_NAME);
        assertThat(flagged).isEmpty();
    }

    @Test
    void importData_emptyApi_apiEmptyState() throws Exception {
        setUpImportDefaultModel();
        stubApiDiscovery(COMPONENT_A1_NAME,ZonedDateTime.now().minusDays(1),
                List.of()); // empty endpoints

        dataImportScheduler.update();

        List<Map<String, Object>> states = querySecscanStates(COMPONENT_A1_NAME);
        assertThat(states).hasSize(1);
        assertThat((String) states.getFirst().get("scan_message")).contains("No HTTP endpoints found");

        List<Map<String, Object>> flagged = queryFlaggedEndpoints(COMPONENT_A1_NAME);
        assertThat(flagged).isEmpty();
    }

    @Test
    void importData_apiIgnored_apiIgnoredState() throws Exception {
        setUpImportDefaultModel();
        when(apiIgnoreFilter.shouldIgnoreApi(argThat(api ->
                api != null && COMPONENT_A1_NAME.equals(api.systemComponentName()))))
                .thenReturn(SystemComponentHttpApiIgnoreFilter.Result.ignoredWithReason("test ignore"));
        stubApiDiscovery(COMPONENT_A1_NAME,ZonedDateTime.now().minusDays(1),
                List.of(endpoint("GET", "/api/resource")));

        dataImportScheduler.update();

        List<Map<String, Object>> states = querySecscanStates(COMPONENT_A1_NAME);
        assertThat(states).hasSize(1);
        String scanMessage = (String) states.getFirst().get("scan_message");
        assertThat(scanMessage).contains("Ignoring").contains("test ignore");

        List<Map<String, Object>> flagged = queryFlaggedEndpoints(COMPONENT_A1_NAME);
        assertThat(flagged).isEmpty();
    }

    @Test
    void importData_scanDataUpToDate_existingDataUnchanged() throws Exception {
        setUpImportDefaultModel();
        ZonedDateTime lastUpdated = ZonedDateTime.parse("2026-01-01T00:00:00Z");
        stubApiDiscovery(COMPONENT_A1_NAME,lastUpdated, List.of(endpoint("GET", "/api/resource")));
        stubEndpointResponse("/services/" + COMPONENT_A1_NAME + "/api/resource", "GET", 200);

        // First import: creates scan data with scanTimestamp > lastUpdated
        dataImportScheduler.update();

        List<Map<String, Object>> statesAfterFirst = querySecscanStates(COMPONENT_A1_NAME);
        assertThat(statesAfterFirst).hasSize(1);
        assertThat((String) statesAfterFirst.getFirst().get("scan_message")).contains("flagged 1 endpoints");
        Object firstScanTimestamp = statesAfterFirst.getFirst().get("scan_timestamp");

        List<Map<String, Object>> flaggedAfterFirst = queryFlaggedEndpoints(COMPONENT_A1_NAME);
        assertThat(flaggedAfterFirst).hasSize(1);

        // Second import: same lastUpdated, scan data should be up to date
        secscanMockServer.resetAll();
        setUpImportDefaultModel();
        stubApiDiscovery(COMPONENT_A1_NAME,lastUpdated, List.of(endpoint("GET", "/api/resource")));
        // No endpoint stubs needed since scan should be skipped

        dataImportScheduler.update();

        // Verify state and flagged endpoints unchanged
        List<Map<String, Object>> statesAfterSecond = querySecscanStates(COMPONENT_A1_NAME);
        assertThat(statesAfterSecond).hasSize(1);
        assertThat(statesAfterSecond.getFirst()).containsEntry("scan_timestamp", firstScanTimestamp);

        List<Map<String, Object>> flaggedAfterSecond = queryFlaggedEndpoints(COMPONENT_A1_NAME);
        assertThat(flaggedAfterSecond).hasSize(1);
    }

    @Test
    void importData_rescanWithEndpointChanges_dataUpdated() throws Exception {
        setUpImportDefaultModel();
        // First import: 1 endpoint, flagged
        stubApiDiscovery(COMPONENT_A1_NAME,ZonedDateTime.now().minusDays(2),
                List.of(endpoint("GET", "/api/old-endpoint")));
        stubEndpointResponse("/services/" + COMPONENT_A1_NAME + "/api/old-endpoint", "GET", 200);

        dataImportScheduler.update();

        List<Map<String, Object>> flaggedAfterFirst = queryFlaggedEndpoints(COMPONENT_A1_NAME);
        assertThat(flaggedAfterFirst).hasSize(1);
        assertThat(flaggedAfterFirst.getFirst()).containsEntry("path", "/api/old-endpoint");

        // Second import: 3 endpoints, 2 flagged (lastUpdated must be after first scan timestamp to trigger rescan)
        secscanMockServer.resetAll();
        setUpImportDefaultModel();
        stubApiDiscovery(COMPONENT_A1_NAME, ZonedDateTime.now().plusDays(1),
                List.of(endpoint("GET", "/api/new1"), endpoint("POST", "/api/new2"), endpoint("DELETE", "/api/new3")));
        stubEndpointResponse("/services/" + COMPONENT_A1_NAME + "/api/new1", "GET", 200);
        stubEndpointResponse("/services/" + COMPONENT_A1_NAME + "/api/new2", "POST", 200);
        stubEndpointResponse("/services/" + COMPONENT_A1_NAME + "/api/new3", "DELETE", 401);

        dataImportScheduler.update();

        List<Map<String, Object>> flaggedAfterSecond = queryFlaggedEndpoints(COMPONENT_A1_NAME);
        assertThat(flaggedAfterSecond)
                .hasSize(2)
                .extracting(row -> row.get("method"), row -> row.get("path"))
                .containsExactlyInAnyOrder(
                        tuple("GET", "/api/new1"),
                        tuple("POST", "/api/new2"));
    }

    @Test
    void importData_previouslyFlaggedNowSecured_flaggedEndpointsRemoved() throws Exception {
        setUpImportDefaultModel();
        // First import: 2 endpoints, both flagged
        stubApiDiscovery(COMPONENT_A1_NAME, ZonedDateTime.now().minusDays(2),
                List.of(endpoint("GET", "/api/ep1"), endpoint("PUT", "/api/ep2")));
        stubEndpointResponse("/services/" + COMPONENT_A1_NAME + "/api/ep1", "GET", 200);
        stubEndpointResponse("/services/" + COMPONENT_A1_NAME + "/api/ep2", "PUT", 200);

        dataImportScheduler.update();

        assertThat(queryFlaggedEndpoints(COMPONENT_A1_NAME)).hasSize(2);

        // Second import: same endpoints, now secured (lastUpdated must be after first scan timestamp to trigger rescan)
        secscanMockServer.resetAll();
        setUpImportDefaultModel();
        stubApiDiscovery(COMPONENT_A1_NAME, ZonedDateTime.now().plusDays(1),
                List.of(endpoint("GET", "/api/ep1"), endpoint("PUT", "/api/ep2")));
        stubEndpointResponse("/services/" + COMPONENT_A1_NAME + "/api/ep1", "GET", 401);
        stubEndpointResponse("/services/" + COMPONENT_A1_NAME + "/api/ep2", "PUT", 401);

        dataImportScheduler.update();

        List<Map<String, Object>> states = querySecscanStates(COMPONENT_A1_NAME);
        assertThat(states).hasSize(1);
        assertThat((String) states.getFirst().get("scan_message")).contains("did not flag any endpoint");

        assertThat(queryFlaggedEndpoints(COMPONENT_A1_NAME)).isEmpty();
    }

    @Test
    void importData_componentRemovedFromModel_secscanDataDeleted() throws Exception {
        // First: default model (9 components) with stubs
        setUpImportDefaultModel();
        for (String componentName : DEFAULT_MODEL_COMPONENT_NAMES) {
            stubApiDiscovery(componentName,ZonedDateTime.now().minusDays(1),
                    List.of(endpoint("GET", "/api/resource")));
            stubEndpointResponse("/services/" + componentName + "/api/resource", "GET", 200);
        }

        dataImportScheduler.update();

        assertThat(querySecscanStates()).hasSize(DEFAULT_MODEL_COMPONENT_NAMES.size());

        // Second: less model (4 components)
        secscanMockServer.resetAll();
        setUpImportModelLess();
        for (String componentName : LESS_MODEL_COMPONENT_NAMES) {
            stubApiDiscovery(componentName,ZonedDateTime.now().plusDays(1),
                    List.of(endpoint("GET", "/api/resource")));
            stubEndpointResponse("/services/" + componentName + "/api/resource", "GET", 200);
        }

        dataImportScheduler.update();

        // Verify removed components have no secscan data
        for (String removedComponent : REMOVED_COMPONENTS) {
            assertThat(querySecscanStates(removedComponent))
                    .as("secscan_state should be deleted for removed component '%s'", removedComponent)
                    .isEmpty();
            assertThat(queryFlaggedEndpoints(removedComponent))
                    .as("flagged endpoints should be deleted for removed component '%s'", removedComponent)
                    .isEmpty();
        }

        // Verify remaining components still have secscan data
        for (String remainingComponent : LESS_MODEL_COMPONENT_NAMES) {
            assertThat(querySecscanStates(remainingComponent))
                    .as("secscan_state should exist for remaining component '%s'", remainingComponent)
                    .hasSize(1);
        }
    }

    @Test
    void importData_componentAddedToModel_secscanDataCreated() throws Exception {
        // First: less model (4 components)
        setUpImportModelLess();
        for (String componentName : LESS_MODEL_COMPONENT_NAMES) {
            stubApiDiscovery(componentName,ZonedDateTime.now().minusDays(1),
                    List.of(endpoint("GET", "/api/resource")));
            stubEndpointResponse("/services/" + componentName + "/api/resource", "GET", 200);
        }

        dataImportScheduler.update();

        assertThat(querySecscanStates()).hasSize(LESS_MODEL_COMPONENT_NAMES.size());

        // Second: default model (9 components)
        secscanMockServer.resetAll();
        setUpImportDefaultModel();
        for (String componentName : DEFAULT_MODEL_COMPONENT_NAMES) {
            stubApiDiscovery(componentName,ZonedDateTime.now().plusDays(1),
                    List.of(endpoint("GET", "/api/resource")));
            stubEndpointResponse("/services/" + componentName + "/api/resource", "GET", 200);
        }

        dataImportScheduler.update();

        // Verify all components including newly added ones have secscan data
        for (String componentName : DEFAULT_MODEL_COMPONENT_NAMES) {
            assertThat(querySecscanStates(componentName))
                    .as("secscan_state should exist for component '%s'", componentName)
                    .hasSize(1);
        }
    }

    private void stubApiDiscovery(String componentName, ZonedDateTime lastUpdated,
                                  List<Map<String, String>> restApis) throws Exception {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("serverUrl", componentServerUrl(componentName));
        response.put("lastUpdated", lastUpdated.toString());
        response.put("version", "1.0.0");
        response.put("restApis", restApis);

        secscanMockServer.stubFor(get(urlEqualTo("/api-discovery/dev/" + componentName))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(response))));
    }

    private static void stubEndpointResponse(String path, String method, int statusCode) {
        if ("GET".equalsIgnoreCase(method)) {
            secscanMockServer.stubFor(get(urlEqualTo(path))
                    .willReturn(aResponse().withStatus(statusCode)));
        } else if ("POST".equalsIgnoreCase(method)) {
            secscanMockServer.stubFor(post(urlEqualTo(path))
                    .willReturn(aResponse().withStatus(statusCode)));
        } else if ("PUT".equalsIgnoreCase(method)) {
            secscanMockServer.stubFor(put(urlEqualTo(path))
                    .willReturn(aResponse().withStatus(statusCode)));
        } else if ("DELETE".equalsIgnoreCase(method)) {
            secscanMockServer.stubFor(delete(urlEqualTo(path))
                    .willReturn(aResponse().withStatus(statusCode)));
        }
    }

    private static Map<String, String> endpoint(String method, String path) {
        Map<String, String> ep = new LinkedHashMap<>();
        ep.put("method", method);
        ep.put("path", path);
        return ep;
    }

    private static String componentServerUrl(String slug) {
        return secscanMockServer.baseUrl() + "/services/" + slug;
    }

    private List<Map<String, Object>> querySecscanStates() {
        return jdbcTemplate.queryForList("SELECT * FROM secscan_state");
    }

    private List<Map<String, Object>> querySecscanStates(String componentName) {
        Long componentId = findSystemComponentId(componentName);
        if (componentId == null) {
            return List.of();
        }
        return jdbcTemplate.queryForList(
                "SELECT * FROM secscan_state WHERE system_component_id = ?", componentId);
    }

    private List<Map<String, Object>> queryFlaggedEndpoints(String componentName) {
        Long componentId = findSystemComponentId(componentName);
        if (componentId == null) {
            return List.of();
        }
        return jdbcTemplate.queryForList(
                "SELECT * FROM secscan_flagged_endpoint WHERE system_component_id = ?", componentId);
    }

    private Long findSystemComponentId(String componentName) {
        List<Long> ids = jdbcTemplate.queryForList(
                "SELECT id FROM system_component WHERE name = ?", Long.class, componentName);
        return ids.isEmpty() ? null : ids.getFirst();
    }

    private void cleanSecscanTables() {
        jdbcTemplate.execute("TRUNCATE TABLE secscan_flagged_endpoint");
        jdbcTemplate.execute("TRUNCATE TABLE secscan_state");
    }
}
