package ch.admin.bit.jeap.governance.prometheus.persistence;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.prometheus.domain.PromQueryType;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeries;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeriesSample;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PromTimeSeriesRepositoryImpl.class)
@Testcontainers
class PromTimeSeriesRepositoryImplTest {

    @SuppressWarnings("unused")
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:17-alpine").asCompatibleSubstituteFor("postgres:17-alpine"));

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PromTimeSeriesRepositoryImpl repository;

    private SystemComponent componentA;
    private SystemComponent componentB;

    @BeforeEach
    void setUp() {
        componentA = persistSystemComponent("Component A");
        componentB = persistSystemComponent("Component B");
    }

    @Test
    void saveAll() {
        PromTimeSeries ts1 = createTimeSeries(PromQueryType.JEAP_JAVA_VERSION, componentA,
                Map.of("version", "21"), List.of("1234567890", "1"));
        PromTimeSeries ts2 = createTimeSeries(PromQueryType.JEAP_DEPENDENCY_VERSION, componentA,
                Map.of("name", "spring-boot", "version", "3.2.0"), List.of("1234567891", "2"));

        repository.saveAll(List.of(ts1, ts2));
        flushAndClear();

        PromTimeSeries found1 = entityManager.find(PromTimeSeries.class, ts1.getId());
        assertThat(found1).isNotNull();
        assertThat(found1.getPrometheusQueryType()).isEqualTo(PromQueryType.JEAP_JAVA_VERSION);
        assertThat(found1.getSystemComponentId()).isEqualTo(componentA.getId());
        assertThat(found1.getSample().metric()).containsExactlyInAnyOrderEntriesOf(Map.of("version", "21"));
        assertThat(found1.getSample().value()).containsExactly("1234567890", "1");

        PromTimeSeries found2 = entityManager.find(PromTimeSeries.class, ts2.getId());
        assertThat(found2).isNotNull();
        assertThat(found2.getPrometheusQueryType()).isEqualTo(PromQueryType.JEAP_DEPENDENCY_VERSION);
        assertThat(found2.getSample().metric()).containsExactlyInAnyOrderEntriesOf(
                Map.of("name", "spring-boot", "version", "3.2.0"));
        assertThat(found2.getSample().value()).containsExactly("1234567891", "2");
    }

    @Test
    void deleteBySystemComponentId() {
        PromTimeSeries tsA1 = createTimeSeries(PromQueryType.JEAP_JAVA_VERSION, componentA,
                Map.of("version", "21"), List.of("1234567890", "1"));
        PromTimeSeries tsA2 = createTimeSeries(PromQueryType.JEAP_DEPENDENCY_VERSION, componentA,
                Map.of("name", "spring-boot", "version", "3.2.0"), List.of("1234567891", "2"));
        PromTimeSeries tsB1 = createTimeSeries(PromQueryType.JEAP_JAVA_VERSION, componentB,
                Map.of("version", "17"), List.of("1234567892", "3"));
        entityManager.persist(tsA1);
        entityManager.persist(tsA2);
        entityManager.persist(tsB1);
        flushAndClear();

        int deletedCount = repository.deleteBySystemComponentId(componentA.getId());

        assertThat(deletedCount).isEqualTo(2);
        assertThat(entityManager.find(PromTimeSeries.class, tsA1.getId())).isNull();
        assertThat(entityManager.find(PromTimeSeries.class, tsA2.getId())).isNull();
        assertThat(entityManager.find(PromTimeSeries.class, tsB1.getId())).isNotNull();
    }

    @Test
    void deleteBySystemComponentId_noMatch() {
        PromTimeSeries tsB1 = createTimeSeries(PromQueryType.JEAP_JAVA_VERSION, componentB,
                Map.of("version", "17"), List.of("1234567890", "1"));
        entityManager.persist(tsB1);
        flushAndClear();

        int deletedCount = repository.deleteBySystemComponentId(componentA.getId());

        assertThat(deletedCount).isZero();
        assertThat(entityManager.find(PromTimeSeries.class, tsB1.getId())).isNotNull();
    }

    @Test
    void findBy() {
        PromTimeSeries tsJavaA = createTimeSeries(PromQueryType.JEAP_JAVA_VERSION, componentA,
                Map.of("version", "21"), List.of("1234567890", "1"));
        PromTimeSeries tsDepA = createTimeSeries(PromQueryType.JEAP_DEPENDENCY_VERSION, componentA,
                Map.of("name", "spring-boot", "version", "3.2.0"), List.of("1234567891", "2"));
        PromTimeSeries tsJavaB = createTimeSeries(PromQueryType.JEAP_JAVA_VERSION, componentB,
                Map.of("version", "17"), List.of("1234567892", "3"));
        entityManager.persist(tsJavaA);
        entityManager.persist(tsDepA);
        entityManager.persist(tsJavaB);
        flushAndClear();

        List<PromTimeSeries> result = repository.findBy(PromQueryType.JEAP_JAVA_VERSION, componentA.getId());

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getSystemComponentId()).isEqualTo(componentA.getId());
        assertThat(result.getFirst().getPrometheusQueryType()).isEqualTo(PromQueryType.JEAP_JAVA_VERSION);
        assertThat(result.getFirst().getSample().metric()).containsExactlyInAnyOrderEntriesOf(Map.of("version", "21"));
    }

    @Test
    void findBy_noMatch() {
        PromTimeSeries tsJavaA = createTimeSeries(PromQueryType.JEAP_JAVA_VERSION, componentA,
                Map.of("version", "21"), List.of("1234567890", "1"));
        entityManager.persist(tsJavaA);
        flushAndClear();

        List<PromTimeSeries> resultDependencyVersionComponentA = repository.findBy(PromQueryType.JEAP_DEPENDENCY_VERSION, componentA.getId());
        assertThat(resultDependencyVersionComponentA).isEmpty();

        List<PromTimeSeries> resultJavaVersionComponentB = repository.findBy(PromQueryType.JEAP_JAVA_VERSION, componentB.getId());
        assertThat(resultJavaVersionComponentB).isEmpty();

        List<PromTimeSeries> resultDependencyVersionComponentB = repository.findBy(PromQueryType.JEAP_DEPENDENCY_VERSION, componentB.getId());
        assertThat(resultDependencyVersionComponentB).isEmpty();
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

    private PromTimeSeries createTimeSeries(PromQueryType queryType, SystemComponent systemComponent,
                                            Map<String, String> metric, List<String> value) {
        return PromTimeSeries.builder()
                .prometheusQueryType(queryType)
                .queryTimestamp(ZonedDateTime.now())
                .systemComponentId(systemComponent.getId())
                .sample(new PromTimeSeriesSample(metric, value))
                .build();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
