package ch.admin.bit.jeap.governance.prometheus.datadeletion;

import ch.admin.bit.jeap.governance.domain.SystemComponentRepository;
import ch.admin.bit.jeap.governance.prometheus.PrometheusAutoconfiguration;
import ch.admin.bit.jeap.governance.prometheus.amp.AmazonManagedPromClient;
import ch.admin.bit.jeap.governance.prometheus.dataimport.PrometheusImporter;
import ch.admin.bit.jeap.governance.prometheus.domain.PromQueryType;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeries;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeriesSample;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DataJpaTest
@Import(PrometheusAutoconfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class PrometheusComponentDeletionListenerTest {

    @SuppressWarnings("unused")
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @MockitoBean
    private SystemComponentRepository systemComponentRepository;

    // not needed by this test of the data deletion listener
    @MockitoBean
    private PrometheusImporter prometheusImporter;

    // not needed by this test of the data deletion listener
    @MockitoBean
    private AmazonManagedPromClient amazonManagedPromClient;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PrometheusComponentDeletionListener deletionListener;

    @Test
    void preComponentDeletion_deletesRelatedPrometheusData() {
        String componentName = "my-service";
        long componentId = 42L;
        persistTimeSeries(componentName);
        persistTimeSeries(componentName);
        persistTimeSeries("other-service");
        assertThat(findAllTimeSeries()).hasSize(3);
        when(systemComponentRepository.findSystemComponentNameById(componentId))
                .thenReturn(Optional.of(componentName));

        deletionListener.preComponentDeletion(componentId);

        List<PromTimeSeries> remaining = findAllTimeSeries();
        assertThat(remaining).hasSize(1);
        assertThat(remaining.getFirst().getSystemComponentName()).isEqualTo("other-service");
    }

    @Test
    void preComponentDeletion_withNullId_doesNothing() {
        persistTimeSeries("my-service");

        deletionListener.preComponentDeletion(null);

        assertThat(findAllTimeSeries()).hasSize(1);
    }

    @Test
    void preComponentDeletion_withUnknownId_doesNothing() {
        persistTimeSeries("my-service");
        when(systemComponentRepository.findSystemComponentNameById(999L))
                .thenReturn(Optional.empty());

        deletionListener.preComponentDeletion(999L);

        assertThat(findAllTimeSeries()).hasSize(1);
    }

    @Test
    void preComponentDeletion_withNoMatchingData_deletesNothing() {
        persistTimeSeries("other-service");
        when(systemComponentRepository.findSystemComponentNameById(42L))
                .thenReturn(Optional.of("non-existing-service"));

        deletionListener.preComponentDeletion(42L);

        assertThat(findAllTimeSeries()).hasSize(1);
    }

    private void persistTimeSeries(String systemComponentName) {
        PromTimeSeries timeSeries = PromTimeSeries.builder()
                .prometheusQueryType(PromQueryType.JEAP_JAVA_VERSION)
                .queryTimestamp(ZonedDateTime.now())
                .systemComponentName(systemComponentName)
                .sample(new PromTimeSeriesSample(Map.of("service", systemComponentName), List.of("1770812157.00", "25.00")))
                .build();
        entityManager.persistAndFlush(timeSeries);
        entityManager.clear();
    }

    private List<PromTimeSeries> findAllTimeSeries() {
        return entityManager.getEntityManager()
                .createQuery("SELECT p FROM PromTimeSeries p", PromTimeSeries.class)
                .getResultList();
    }
}
