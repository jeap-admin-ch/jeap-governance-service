package ch.admin.bit.jeap.governance.web;

import ch.admin.bit.jeap.governance.dataimport.DataImportScheduler;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
@AutoConfigureObservability
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "management.endpoint.prometheus.enabled=true",
        "management.endpoints.web.exposure.include=*"})
class ArchRepoMetricsIT extends ArchRepoMockIntegrationTestBase {

    @LocalServerPort
    private int localServerPort;

    @Autowired
    private DataImportScheduler dataImportScheduler;

    @Test
    void testArchRepoMetrics() throws Exception {
        setUpImportDefaultModel();

        dataImportScheduler.update();

        Response response = RestAssured.given()
                .basePath("/jeap-governance-service-test")
                .port(localServerPort)
                .get("/actuator/prometheus");
        assertEquals(200, response.getStatusCode());
        final String metrics = response.getBody().asString();
        assertThat(metrics).contains(
                "jeap_governance_service_data_import_duration_seconds_sum{data_source_connector=\"ApiDocVersionImporter\"",
                "jeap_governance_service_data_import_duration_seconds_sum{data_source_connector=\"ArchRepoSystemImporter\"",
                "jeap_governance_service_data_import_duration_seconds_sum{data_source_connector=\"DatabaseSchemaVersionImporter\"",
                "jeap_governance_service_data_import_duration_seconds_sum{data_source_connector=\"ReactionGraphImporter\"",
                "jeap_governance_service_data_import_duration_seconds_sum{data_source_connector=\"RestApiRelationWithoutPactImporter\"",
                "jeap_governance_service_data_import_last_run_from_minutes"
        );
    }

}
