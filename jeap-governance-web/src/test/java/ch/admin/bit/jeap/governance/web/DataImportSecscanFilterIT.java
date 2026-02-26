package ch.admin.bit.jeap.governance.web;

import ch.admin.bit.jeap.governance.dataimport.DataImportScheduler;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static ch.admin.bit.jeap.governance.web.ImportModelHelper.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Integration test for the secscan data import pipeline with the REAL
 * {@code ConfigurableSystemComponentHttpApiIgnoreFilter} (no mock).
 * Tests that filter parameters (exempt-methods, exempt-paths, exempt-endpoints,
 * exempt-api-url-not-containing, exempt-api-url-containing) and component exemptions
 * are applied correctly during the scan pipeline.
 */
@SpringBootTest(properties = {"jeap.governance.secscan.enabled=true"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("dataimport-secscan-filter-it")
class DataImportSecscanFilterIT extends GovernanceIntegrationTestBase {

    protected static WireMockServer secscanMockServer;

    @Autowired
    private DataImportScheduler dataImportScheduler;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    // NO @MockitoBean for SystemComponentHttpApiIgnoreFilter — uses real filter

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
    }

    @Test
    void endpointWithExemptMethod_notFlagged() throws Exception {
        setUpImportDefaultModel();
        // OPTIONS /api/opts should be ignored (exempt-methods: OPTIONS), GET /api/res should be flagged
        String serverPath = ingressPath(COMPONENT_A1_NAME);
        stubApiDiscovery(COMPONENT_A1_NAME, secscanMockServer.baseUrl() + serverPath,
                List.of(endpoint("OPTIONS", "/api/opts"), endpoint("GET", "/api/res")));
        stubEndpointResponse(serverPath + "/api/opts", "OPTIONS", 200);
        stubEndpointResponse(serverPath + "/api/res", "GET", 200);

        dataImportScheduler.update();

        List<Map<String, Object>> flagged = queryFlaggedEndpoints(COMPONENT_A1_NAME);
        assertThat(flagged)
                .hasSize(1)
                .extracting(row -> row.get("method"), row -> row.get("path"))
                .containsExactly(tuple("GET", "/api/res"));
    }

    @Test
    void endpointWithExemptPath_notFlagged() throws Exception {
        setUpImportDefaultModel();
        // GET /public/health should be ignored (exempt-paths: /public/*), GET /api/res should be flagged
        String serverPath = ingressPath(COMPONENT_A2_NAME);
        stubApiDiscovery(COMPONENT_A2_NAME, secscanMockServer.baseUrl() + serverPath,
                List.of(endpoint("GET", "/public/health"), endpoint("GET", "/api/res")));
        stubEndpointResponse(serverPath + "/public/health", "GET", 200);
        stubEndpointResponse(serverPath + "/api/res", "GET", 200);

        dataImportScheduler.update();

        List<Map<String, Object>> flagged = queryFlaggedEndpoints(COMPONENT_A2_NAME);
        assertThat(flagged)
                .hasSize(1)
                .extracting(row -> row.get("method"), row -> row.get("path"))
                .containsExactly(tuple("GET", "/api/res"));
    }

    @Test
    void endpointWithExemptEndpoint_notFlagged() throws Exception {
        setUpImportDefaultModel();
        // DELETE /admin/users should be ignored (exempt-endpoints: DELETE:/admin/*), GET /api/res should be flagged
        String serverPath = ingressPath(COMPONENT_B1_NAME);
        stubApiDiscovery(COMPONENT_B1_NAME, secscanMockServer.baseUrl() + serverPath,
                List.of(endpoint("DELETE", "/admin/users"), endpoint("GET", "/api/res")));
        stubEndpointResponse(serverPath + "/admin/users", "DELETE", 200);
        stubEndpointResponse(serverPath + "/api/res", "GET", 200);

        dataImportScheduler.update();

        List<Map<String, Object>> flagged = queryFlaggedEndpoints(COMPONENT_B1_NAME);
        assertThat(flagged)
                .hasSize(1)
                .extracting(row -> row.get("method"), row -> row.get("path"))
                .containsExactly(tuple("GET", "/api/res"));
    }

    @Test
    void apiUrlNotContainingRequired_apiIgnored() throws Exception {
        setUpImportDefaultModel();
        // URL does NOT contain "ingress" or "internal-csp" -> API is ignored entirely
        String serverPath = nonIngressPath(COMPONENT_B2_NAME);
        stubApiDiscovery(COMPONENT_B2_NAME, secscanMockServer.baseUrl() + serverPath,
                List.of(endpoint("GET", "/api/res")));

        dataImportScheduler.update();

        List<Map<String, Object>> states = querySecscanStates(COMPONENT_B2_NAME);
        assertThat(states).hasSize(1);
        assertThat((String) states.getFirst().get("scan_message")).contains("Ignoring");

        List<Map<String, Object>> flagged = queryFlaggedEndpoints(COMPONENT_B2_NAME);
        assertThat(flagged).isEmpty();
    }

    @Test
    void apiUrlContainingExemptString_apiIgnored() throws Exception {
        setUpImportDefaultModel();
        // URL contains "egress" -> API is ignored entirely
        String serverPath = egressPath(COMPONENT_C1_NAME);
        stubApiDiscovery(COMPONENT_C1_NAME, secscanMockServer.baseUrl() + serverPath,
                List.of(endpoint("GET", "/api/res")));

        dataImportScheduler.update();

        List<Map<String, Object>> states = querySecscanStates(COMPONENT_C1_NAME);
        assertThat(states).hasSize(1);
        assertThat((String) states.getFirst().get("scan_message")).contains("Ignoring");

        List<Map<String, Object>> flagged = queryFlaggedEndpoints(COMPONENT_C1_NAME);
        assertThat(flagged).isEmpty();
    }

    @Test
    void exemptedComponent_allEndpointsIgnored() throws Exception {
        setUpImportDefaultModel();
        // sysa-comp3-svc is permanently exempted — all endpoints should be ignored
        String serverPath = ingressPath(COMPONENT_A3_NAME);
        stubApiDiscovery(COMPONENT_A3_NAME, secscanMockServer.baseUrl() + serverPath,
                List.of(endpoint("GET", "/api/open")));
        stubEndpointResponse(serverPath + "/api/open", "GET", 200);

        dataImportScheduler.update();

        List<Map<String, Object>> states = querySecscanStates(COMPONENT_A3_NAME);
        assertThat(states).hasSize(1);
        assertThat((String) states.getFirst().get("scan_message")).contains("did not flag any endpoint");

        List<Map<String, Object>> flagged = queryFlaggedEndpoints(COMPONENT_A3_NAME);
        assertThat(flagged).isEmpty();
    }

    @Test
    void exemptedUntilComponent_allEndpointsIgnored() throws Exception {
        setUpImportDefaultModel();
        // sysb-comp3-svc is temporarily exempted (until 2099-12-31) — all endpoints should be ignored
        String serverPath = ingressPath(COMPONENT_B3_NAME);
        stubApiDiscovery(COMPONENT_B3_NAME, secscanMockServer.baseUrl() + serverPath,
                List.of(endpoint("GET", "/api/open")));
        stubEndpointResponse(serverPath + "/api/open", "GET", 200);

        dataImportScheduler.update();

        List<Map<String, Object>> states = querySecscanStates(COMPONENT_B3_NAME);
        assertThat(states).hasSize(1);
        assertThat((String) states.getFirst().get("scan_message")).contains("did not flag any endpoint");

        List<Map<String, Object>> flagged = queryFlaggedEndpoints(COMPONENT_B3_NAME);
        assertThat(flagged).isEmpty();
    }

    @Test
    void securedEndpointReturning401_notFlagged() throws Exception {
        setUpImportDefaultModel();
        // GET /api/secured returns 401, GET /api/open returns 200
        String serverPath = ingressPath(COMPONENT_A1_NAME);
        stubApiDiscovery(COMPONENT_A1_NAME, secscanMockServer.baseUrl() + serverPath,
                List.of(endpoint("GET", "/api/secured"), endpoint("GET", "/api/open")));
        stubEndpointResponse(serverPath + "/api/secured", "GET", 401);
        stubEndpointResponse(serverPath + "/api/open", "GET", 200);

        dataImportScheduler.update();

        List<Map<String, Object>> flagged = queryFlaggedEndpoints(COMPONENT_A1_NAME);
        // Only the open endpoint should be flagged, the secured one (401) should not
        assertThat(flagged)
                .hasSize(1)
                .extracting(row -> row.get("method"), row -> row.get("path"))
                .containsExactly(tuple("GET", "/api/open"));
    }

    // --- Helper methods ---

    private void stubApiDiscovery(String componentName, String serverUrl,
                                  List<Map<String, String>> restApis) throws Exception {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("serverUrl", serverUrl);
        response.put("lastUpdated", ZonedDateTime.now().minusDays(1).toString());
        response.put("version", "1.0.0");
        response.put("restApis", restApis);

        secscanMockServer.stubFor(get(urlEqualTo("/api-discovery/ref/" + componentName))
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
        } else if ("OPTIONS".equalsIgnoreCase(method)) {
            secscanMockServer.stubFor(options(urlEqualTo(path))
                    .willReturn(aResponse().withStatus(statusCode)));
        }
    }

    private static Map<String, String> endpoint(String method, String path) {
        Map<String, String> ep = new LinkedHashMap<>();
        ep.put("method", method);
        ep.put("path", path);
        return ep;
    }

    private static String ingressPath(String slug) {
        return "/services/ingress-" + slug;
    }

    private static String nonIngressPath(String slug) {
        return "/services/" + slug;
    }

    private static String egressPath(String slug) {
        return "/services/egress-" + slug;
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
