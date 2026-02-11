package ch.admin.bit.jeap.governance.web;

import ch.admin.bit.jeap.governance.dataimport.DataImportScheduler;
import ch.admin.bit.jeap.governance.prometheus.domain.PromClient;
import ch.admin.bit.jeap.governance.prometheus.domain.PromQueryType;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeries;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeriesSample;
import ch.admin.bit.jeap.governance.prometheus.persistence.JpaPromTimeSeriesRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static ch.admin.bit.jeap.governance.web.ImportModelHelper.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {"jeap.governance.prometheus.enabled=true"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DataImportPrometheusIT extends GovernanceIntegrationTestBase {

    private static final Set<String> REMOVED_COMPONENTS = removedComponents();

    private static Set<String> removedComponents() {
        Set<String> removed = new HashSet<>(DEFAULT_MODEL_COMPONENT_NAMES);
        removed.removeAll(LESS_MODEL_COMPONENT_NAMES);
        return Collections.unmodifiableSet(removed);
    }

    @Autowired
    private JpaPromTimeSeriesRepository promTimeSeriesRepository;

    @Autowired
    private DataImportScheduler dataImportScheduler;

    @MockitoBean
    private PromClient promClient;

    @Test
    void synchronizeArchRepoModel_shouldImportPrometheusTimeSeries_initial() throws Exception {
        setUpImportDefaultModel();
        stubPromClient();

        dataImportScheduler.update();

        List<PromTimeSeries> all = toList(promTimeSeriesRepository.findAll());
        assertTimeSeriesForComponents(all, DEFAULT_MODEL_COMPONENT_NAMES);
    }

    @Test
    void synchronizeArchRepoModel_shouldImportPrometheusTimeSeries_deletedSystemsAndComponents() throws Exception {
        setUpImportDefaultModel();
        stubPromClient();

        dataImportScheduler.update();

        List<PromTimeSeries> all = toList(promTimeSeriesRepository.findAll());
        assertTimeSeriesForComponents(all, DEFAULT_MODEL_COMPONENT_NAMES);

        setUpImportModelLess();

        dataImportScheduler.update();

        List<PromTimeSeries> allAfterDeletion = toList(promTimeSeriesRepository.findAll());
        assertTimeSeriesForComponents(allAfterDeletion, LESS_MODEL_COMPONENT_NAMES);
        assertThat(allAfterDeletion)
                .extracting(PromTimeSeries::getSystemComponentName)
                .doesNotContainAnyElementsOf(REMOVED_COMPONENTS);
    }

    @Test
    void synchronizeArchRepoModel_shouldImportPrometheusTimeSeries_addedSystemsAndComponents() throws Exception {
        setUpImportModelLess();
        stubPromClient();

        dataImportScheduler.update();

        List<PromTimeSeries> all = toList(promTimeSeriesRepository.findAll());
        assertTimeSeriesForComponents(all, LESS_MODEL_COMPONENT_NAMES);

        setUpImportDefaultModel();

        dataImportScheduler.update();

        List<PromTimeSeries> allAfterAddition = toList(promTimeSeriesRepository.findAll());
        assertTimeSeriesForComponents(allAfterAddition, DEFAULT_MODEL_COMPONENT_NAMES);
    }

    /**
     * Asserts that the time series contain exactly the expected instances for the given components:
     * one JAVA_VERSION and two DEPENDENCY_VERSION instances per component, with the correct data.
     */
    private void assertTimeSeriesForComponents(List<PromTimeSeries> timeSeries, Set<String> expectedComponents) {
        int expectedPerComponent = 3; // 1 JAVA_VERSION + 2 DEPENDENCY_VERSION
        assertThat(timeSeries).hasSize(expectedComponents.size() * expectedPerComponent);

        // Verify that exactly the expected components are present, each with exactly 3 time series
        Map<String, Long> countsByComponent = timeSeries.stream()
                .collect(Collectors.groupingBy(PromTimeSeries::getSystemComponentName, Collectors.counting()));
        assertThat(countsByComponent).containsOnlyKeys(expectedComponents.toArray(String[]::new));
        assertThat(countsByComponent.values()).allSatisfy(count -> assertThat(count).isEqualTo(3L));

        for (String componentName : expectedComponents) {
            List<PromTimeSeries> componentSeries = timeSeries.stream()
                    .filter(ts -> ts.getSystemComponentName().equals(componentName))
                    .toList();

            // Each component has exactly 1 JAVA_VERSION record
            List<PromTimeSeries> javaVersionSeries = componentSeries.stream()
                    .filter(ts -> PromQueryType.JEAP_JAVA_VERSION == ts.getPrometheusQueryType())
                    .toList();
            assertThat(javaVersionSeries).hasSize(1);
            PromTimeSeriesSample javaVersionSample = javaVersionSeries.getFirst().getSample();
            assertThat(javaVersionSample.metric()).containsExactlyInAnyOrderEntriesOf(
                    Map.of("service", "test-service", "stage", "prod", "version", "21", "task_revision", "10"));
            assertThat(javaVersionSample.value()).containsExactly("1234567890", "1");

            // Each component has exactly 2 DEPENDENCY_VERSION records
            List<PromTimeSeries> depVersionSeries = componentSeries.stream()
                    .filter(ts -> PromQueryType.JEAP_DEPENDENCY_VERSION == ts.getPrometheusQueryType())
                    .toList();
            assertThat(depVersionSeries).hasSize(2);

            PromTimeSeriesSample springBootSample = depVersionSeries.stream()
                    .map(PromTimeSeries::getSample)
                    .filter(s -> "spring-boot".equals(s.metric().get("name")))
                    .findFirst().orElseThrow();
            assertThat(springBootSample.metric()).containsExactlyInAnyOrderEntriesOf(
                    Map.of("service", "test-service", "stage", "prod", "name", "spring-boot", "version", "3.2.0", "task_revision", "10"));
            assertThat(springBootSample.value()).containsExactly("1234567890", "1");

            PromTimeSeriesSample jeapStarterSample = depVersionSeries.stream()
                    .map(PromTimeSeries::getSample)
                    .filter(s -> "jeap-spring-boot-starter".equals(s.metric().get("name")))
                    .findFirst().orElseThrow();
            assertThat(jeapStarterSample.metric()).containsExactlyInAnyOrderEntriesOf(
                    Map.of("service", "test-service", "stage", "prod", "name", "jeap-spring-boot-starter", "version", "5.1.0", "task_revision", "10"));
            assertThat(jeapStarterSample.value()).containsExactly("1234567891", "2");
        }
    }

    private static List<PromTimeSeries> toList(Iterable<PromTimeSeries> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false).toList();
    }

    private void stubPromClient() {
        // Default: return empty list for all query types (not an error, just no data)
        when(promClient.query(any(), any(), any())).thenReturn(List.of());

        // JAVA_VERSION: one sample with multiple tags and a value
        when(promClient.query(eq(PromQueryType.JEAP_JAVA_VERSION), any(), any()))
                .thenReturn(List.of(new PromTimeSeriesSample(
                        Map.of("service", "test-service", "stage", "prod", "version", "21", "task_revision", "10"),
                        List.of("1234567890", "1"))));

        // DEPENDENCY_VERSION: two samples with multiple tags and distinct values ("1" and "2")
        when(promClient.query(eq(PromQueryType.JEAP_DEPENDENCY_VERSION), any(), any()))
                .thenReturn(List.of(
                        new PromTimeSeriesSample(
                                Map.of("service", "test-service", "stage", "prod", "name", "spring-boot", "version", "3.2.0", "task_revision", "10"),
                                List.of("1234567890", "1")),
                        new PromTimeSeriesSample(
                                Map.of("service", "test-service", "stage", "prod", "name", "jeap-spring-boot-starter", "version", "5.1.0", "task_revision", "10"),
                                List.of("1234567891", "2"))));
    }
}
