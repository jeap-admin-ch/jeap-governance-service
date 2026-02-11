package ch.admin.bit.jeap.governance.prometheus.dataimport;

import ch.admin.bit.jeap.governance.domain.GovernanceProperties;
import ch.admin.bit.jeap.governance.domain.GovernanceServiceEnvironment;
import ch.admin.bit.jeap.governance.domain.SystemComponentRepository;
import ch.admin.bit.jeap.governance.prometheus.PrometheusAutoconfiguration;
import ch.admin.bit.jeap.governance.prometheus.amp.AmazonManagedPromClient;
import ch.admin.bit.jeap.governance.prometheus.datadeletion.PrometheusComponentDeletionListener;
import ch.admin.bit.jeap.governance.prometheus.domain.PromQueryType;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeries;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeriesSample;
import ch.admin.bit.jeap.governance.prometheus.domain.Transactions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManager;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DataJpaTest
@Import(PrometheusAutoconfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class PrometheusImporterTest {

    @SuppressWarnings("unused")
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @MockitoBean
    private SystemComponentRepository systemComponentRepository;

    @MockitoBean
    private AmazonManagedPromClient amazonManagedPromClient;

    @MockitoBean
    private GovernanceProperties governanceProperties;

    // not needed by this test of the importer
    @MockitoBean
    private PrometheusComponentDeletionListener deletionListener;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private Transactions transactions;

    @Autowired
    private PrometheusImporter prometheusImporter;

    @BeforeEach
    void setUp() {
        when(governanceProperties.getEnvironment()).thenReturn(GovernanceServiceEnvironment.DEV);
        // Clean up data committed in separate transactions by the importer
        transactions.inNewTransaction(() ->
                entityManager.createQuery("DELETE FROM PromTimeSeries").executeUpdate()
        );
    }

    @Test
    void importData_importsDataForAllSystemComponents() {
        when(systemComponentRepository.findAllSystemComponentNames())
                .thenReturn(Set.of("service-a", "service-b"));
        PromTimeSeriesSample sampleA = new PromTimeSeriesSample(Map.of("service", "service-a"), List.of("1770812157.00", "1.00"));
        PromTimeSeriesSample sampleB = new PromTimeSeriesSample(Map.of("service", "service-b"), List.of("1770812157.00", "2.00"));
        when(amazonManagedPromClient.query(any(PromQueryType.class), eq(GovernanceServiceEnvironment.DEV), eq("service-a")))
                .thenReturn(List.of(sampleA));
        when(amazonManagedPromClient.query(any(PromQueryType.class), eq(GovernanceServiceEnvironment.DEV), eq("service-b")))
                .thenReturn(List.of(sampleB));

        prometheusImporter.importData();

        List<PromTimeSeries> allTimeSeries = findAllTimeSeriesInNewTransaction();
        int numQueryTypes = PromQueryType.values().length;
        assertThat(allTimeSeries).hasSize(2 * numQueryTypes);
        List<PromTimeSeries> serviceAData = filterByComponent(allTimeSeries, "service-a");
        assertThat(serviceAData).hasSize(numQueryTypes);
        assertThat(serviceAData).allMatch(ts -> ts.getSample().value().equals(List.of("1770812157.00", "1.00")));
        assertThat(serviceAData.stream().map(PromTimeSeries::getPrometheusQueryType).collect(Collectors.toSet()))
                .isEqualTo(allQueryTypes());
        List<PromTimeSeries> serviceBData = filterByComponent(allTimeSeries, "service-b");
        assertThat(serviceBData).hasSize(numQueryTypes);
        assertThat(serviceBData).allMatch(ts -> ts.getSample().value().equals(List.of("1770812157.00", "2.00")));
        assertThat(serviceBData.stream().map(PromTimeSeries::getPrometheusQueryType).collect(Collectors.toSet()))
                .isEqualTo(allQueryTypes());
    }

    @Test
    void importData_replacesExistingDataForSystemComponent() {
        String componentName = "my-service";
        ZonedDateTime oldTimestamp = ZonedDateTime.now().minusDays(1);
        persistTimeSeriesInNewTransaction(componentName, PromQueryType.JEAP_JAVA_VERSION, oldTimestamp, "99.00");
        persistTimeSeriesInNewTransaction(componentName, PromQueryType.JEAP_JAVA_VERSION, oldTimestamp, "88.00");
        assertThat(findAllTimeSeriesInNewTransaction()).hasSize(2);
        when(systemComponentRepository.findAllSystemComponentNames()).thenReturn(Set.of(componentName));
        PromTimeSeriesSample newSample = new PromTimeSeriesSample(Map.of("service", componentName), List.of("1770812157.00", "25.00"));
        when(amazonManagedPromClient.query(any(PromQueryType.class), eq(GovernanceServiceEnvironment.DEV), eq(componentName)))
                .thenReturn(List.of(newSample));

        prometheusImporter.importData();

        List<PromTimeSeries> allTimeSeries = findAllTimeSeriesInNewTransaction();
        // Old data deleted, new data inserted (one per query type)
        int numQueryTypes = PromQueryType.values().length;
        assertThat(allTimeSeries).hasSize(numQueryTypes);
        // All entries have the new sample value, none have the old values
        assertThat(allTimeSeries).allSatisfy(ts -> {
            assertThat(ts.getSystemComponentName()).isEqualTo(componentName);
            assertThat(ts.getSample().value()).isEqualTo(List.of("1770812157.00", "25.00"));
            assertThat(ts.getQueryTimestamp()).isAfter(oldTimestamp);
        });
        assertThat(allTimeSeries.stream().map(PromTimeSeries::getPrometheusQueryType).collect(Collectors.toSet()))
                .isEqualTo(allQueryTypes());
    }

    @Test
    void importData_doesNotAffectDataOfOtherSystemComponents() {
        ZonedDateTime otherServiceTimestamp = ZonedDateTime.now().minusDays(1).truncatedTo(ChronoUnit.MICROS);
        persistTimeSeriesInNewTransaction("other-service", PromQueryType.JEAP_JAVA_VERSION, otherServiceTimestamp, "42.00");
        when(systemComponentRepository.findAllSystemComponentNames()).thenReturn(Set.of("my-service"));
        PromTimeSeriesSample newSample = new PromTimeSeriesSample(Map.of("service", "my-service"), List.of("1770812157.00", "25.00"));
        when(amazonManagedPromClient.query(any(PromQueryType.class), eq(GovernanceServiceEnvironment.DEV), eq("my-service")))
                .thenReturn(List.of(newSample));

        prometheusImporter.importData();

        List<PromTimeSeries> allTimeSeries = findAllTimeSeriesInNewTransaction();
        List<PromTimeSeries> myServiceData = filterByComponent(allTimeSeries, "my-service");
        assertThat(myServiceData).hasSize(PromQueryType.values().length);
        assertThat(myServiceData).allMatch(ts -> ts.getSample().value().equals(List.of("1770812157.00", "25.00")));
        List<PromTimeSeries> otherServiceData = filterByComponent(allTimeSeries, "other-service");
        assertThat(otherServiceData).hasSize(1);
        assertThat(otherServiceData.getFirst().getSample().value()).isEqualTo(List.of("1770812157.00", "42.00"));
        assertThat(otherServiceData.getFirst().getQueryTimestamp().toInstant()).isEqualTo(otherServiceTimestamp.toInstant());
    }

    @Test
    void importData_handlesPartialQueryFailures() {
        String componentName = "my-service";
        when(systemComponentRepository.findAllSystemComponentNames()).thenReturn(Set.of(componentName));

        // All query types fail by default
        when(amazonManagedPromClient.query(any(PromQueryType.class), eq(GovernanceServiceEnvironment.DEV), eq(componentName)))
                .thenThrow(new RuntimeException("query failed"));
        // JAVA_VERSION succeeds
        PromTimeSeriesSample sample = new PromTimeSeriesSample(Map.of("service", componentName), List.of("1770812157.00", "1.00"));
        when(amazonManagedPromClient.query(eq(PromQueryType.JEAP_JAVA_VERSION), eq(GovernanceServiceEnvironment.DEV), eq(componentName)))
                .thenReturn(List.of(sample));

        prometheusImporter.importData();

        List<PromTimeSeries> allTimeSeries = findAllTimeSeriesInNewTransaction();
        assertThat(allTimeSeries).hasSize(1);
        assertThat(allTimeSeries.getFirst().getPrometheusQueryType()).isEqualTo(PromQueryType.JEAP_JAVA_VERSION);
        assertThat(allTimeSeries.getFirst().getSample().value()).isEqualTo(List.of("1770812157.00", "1.00"));
    }

    @Test
    void importData_allQueryTypesFailForComponent_leavesExistingDataUnchanged() {
        String componentName = "my-service";
        ZonedDateTime existingTimestamp = ZonedDateTime.now().minusDays(1).truncatedTo(ChronoUnit.MICROS);
        persistTimeSeriesInNewTransaction(componentName, PromQueryType.JEAP_JAVA_VERSION, existingTimestamp, "77.00");
        assertThat(findAllTimeSeriesInNewTransaction()).hasSize(1);
        when(systemComponentRepository.findAllSystemComponentNames()).thenReturn(Set.of(componentName));
        when(amazonManagedPromClient.query(any(PromQueryType.class), any(), eq(componentName)))
                .thenThrow(new RuntimeException("query failed"));

        prometheusImporter.importData();

        // When all queries fail, the exception is caught and existing data is left unchanged
        List<PromTimeSeries> remaining = findAllTimeSeriesInNewTransaction();
        assertThat(remaining).hasSize(1);
        assertThat(remaining.getFirst().getSample().value()).isEqualTo(List.of("1770812157.00", "77.00"));
        assertThat(remaining.getFirst().getQueryTimestamp().toInstant()).isEqualTo(existingTimestamp.toInstant());
    }

    @Test
    void importData_failureForOneComponentDoesNotAffectOthers() {
        when(systemComponentRepository.findAllSystemComponentNames())
                .thenReturn(Set.of("failing-service", "working-service"));
        when(amazonManagedPromClient.query(any(PromQueryType.class), any(), eq("failing-service")))
                .thenThrow(new RuntimeException("query failed"));
        PromTimeSeriesSample sample = new PromTimeSeriesSample(Map.of("service", "working-service"), List.of("1770812157.00", "1.00"));
        when(amazonManagedPromClient.query(any(PromQueryType.class), any(), eq("working-service")))
                .thenReturn(List.of(sample));

        prometheusImporter.importData();

        List<PromTimeSeries> allTimeSeries = findAllTimeSeriesInNewTransaction();
        int numQueryTypes = PromQueryType.values().length;
        assertThat(allTimeSeries).hasSize(numQueryTypes);
        assertThat(allTimeSeries).allSatisfy(ts -> {
            assertThat(ts.getSystemComponentName()).isEqualTo("working-service");
            assertThat(ts.getSample().value()).isEqualTo(List.of("1770812157.00", "1.00"));
        });
    }

    @Test
    void importData_noSystemComponents_doesNothing() {
        when(systemComponentRepository.findAllSystemComponentNames()).thenReturn(Set.of());

        prometheusImporter.importData();

        assertThat(findAllTimeSeriesInNewTransaction()).isEmpty();
        verifyNoInteractions(amazonManagedPromClient);
    }

    @Test
    void importData_emptyQueryResults_deletesExistingData() {
        String componentName = "my-service";
        persistTimeSeriesInNewTransaction(componentName, PromQueryType.JEAP_JAVA_VERSION, ZonedDateTime.now(), "25.00");
        assertThat(findAllTimeSeriesInNewTransaction()).hasSize(1);
        when(systemComponentRepository.findAllSystemComponentNames()).thenReturn(Set.of(componentName));
        when(amazonManagedPromClient.query(any(PromQueryType.class), any(), eq(componentName)))
                .thenReturn(List.of());

        prometheusImporter.importData();

        assertThat(findAllTimeSeriesInNewTransaction()).isEmpty();
    }

    private void persistTimeSeriesInNewTransaction(String systemComponentName, PromQueryType queryType,
                                                    ZonedDateTime queryTimestamp, String sampleValue) {
        transactions.inNewTransaction(() -> {
            PromTimeSeries timeSeries = PromTimeSeries.builder()
                    .prometheusQueryType(queryType)
                    .queryTimestamp(queryTimestamp)
                    .systemComponentName(systemComponentName)
                    .sample(new PromTimeSeriesSample(Map.of("service", systemComponentName), List.of("1770812157.00", sampleValue)))
                    .build();
            entityManager.persist(timeSeries);
        });
    }

    private static List<PromTimeSeries> filterByComponent(List<PromTimeSeries> timeSeries, String componentName) {
        return timeSeries.stream()
                .filter(ts -> ts.getSystemComponentName().equals(componentName))
                .toList();
    }

    private static Set<PromQueryType> allQueryTypes() {
        return Set.of(PromQueryType.values());
    }

    private List<PromTimeSeries> findAllTimeSeriesInNewTransaction() {
        List<List<PromTimeSeries>> result = new java.util.ArrayList<>();
        transactions.inNewTransaction(() ->
                result.add(entityManager.createQuery("SELECT p FROM PromTimeSeries p", PromTimeSeries.class).getResultList())
        );
        return result.getFirst();
    }
}
