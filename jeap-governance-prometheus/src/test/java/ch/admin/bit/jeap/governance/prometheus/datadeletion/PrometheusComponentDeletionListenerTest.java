package ch.admin.bit.jeap.governance.prometheus.datadeletion;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.SystemComponentRepository;
import ch.admin.bit.jeap.governance.prometheus.PrometheusAutoconfiguration;
import ch.admin.bit.jeap.governance.prometheus.amp.AmazonManagedPromClient;
import ch.admin.bit.jeap.governance.prometheus.dataimport.PrometheusImporter;
import ch.admin.bit.jeap.governance.prometheus.domain.PromQueryType;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeries;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeriesSample;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(PrometheusAutoconfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class PrometheusComponentDeletionListenerTest {

    @SuppressWarnings("unused")
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer(
            DockerImageName.parse("postgres:17-alpine").asCompatibleSubstituteFor("postgres:17-alpine"));

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
        SystemComponent myComponent = persistSystemComponent("my-service");
        SystemComponent otherComponent = persistSystemComponent("other-service");
        persistTimeSeries(myComponent);
        persistTimeSeries(myComponent);
        persistTimeSeries(otherComponent);
        assertThat(findAllTimeSeries()).hasSize(3);

        deletionListener.preComponentDeletion(myComponent.getId());

        List<PromTimeSeries> remaining = findAllTimeSeries();
        assertThat(remaining).hasSize(1);
        assertThat(remaining.getFirst().getSystemComponentId()).isEqualTo(otherComponent.getId());
    }

    @Test
    void preComponentDeletion_withUnknownId_doesNothing() {
        persistTimeSeries(persistSystemComponent("my-service"));

        deletionListener.preComponentDeletion(999L);

        assertThat(findAllTimeSeries()).hasSize(1);
    }

    @Test
    void preComponentDeletion_withNoMatchingData_deletesNothing() {
        SystemComponent otherComponent = persistSystemComponent("other-service");
        SystemComponent emptyComponent = persistSystemComponent("empty-service");
        persistTimeSeries(otherComponent);

        deletionListener.preComponentDeletion(emptyComponent.getId());

        assertThat(findAllTimeSeries()).hasSize(1);
    }

    private SystemComponent persistSystemComponent(String name) {
        SystemComponent component = SystemComponent.builder()
                .name(name)
                .type(ComponentType.BACKEND_SERVICE)
                .build();
        System system = ch.admin.bit.jeap.governance.domain.System.builder()
                .name("system-" + name)
                .aliases(Set.of())
                .systemComponents(List.of(component))
                .build();
        entityManager.persistAndFlush(system);
        return system.getSystemComponents().getFirst();
    }

    private void persistTimeSeries(SystemComponent systemComponent) {
        PromTimeSeries timeSeries = PromTimeSeries.builder()
                .prometheusQueryType(PromQueryType.JEAP_JAVA_VERSION)
                .queryTimestamp(ZonedDateTime.now())
                .systemComponentId(systemComponent.getId())
                .sample(new PromTimeSeriesSample(Map.of("service", systemComponent.getName()), List.of("1770812157.00", "25.00")))
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
