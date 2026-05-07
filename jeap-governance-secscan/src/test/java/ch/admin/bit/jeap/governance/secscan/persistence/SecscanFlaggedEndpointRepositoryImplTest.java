package ch.admin.bit.jeap.governance.secscan.persistence;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.secscan.domain.SecscanFlaggedEndpoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(SecscanFlaggedEndpointRepositoryImpl.class)
class SecscanFlaggedEndpointRepositoryImplTest extends PostgresTestContainerBase {

    private static final String COMPONENT_A = "testa-backend-svc";
    private static final String COMPONENT_B = "testb-api-svc";

    @Autowired
    private SecscanFlaggedEndpointRepositoryImpl repository;

    @Test
    void saveAll() {
        SystemComponent componentA = persistSystemComponent(COMPONENT_A);
        ZonedDateTime scanTimestamp = ZonedDateTime.now().truncatedTo(ChronoUnit.MICROS);
        SecscanFlaggedEndpoint ep1 = SecscanFlaggedEndpoint.builder()
                .systemComponentId(componentA.getId())
                .path("/api/users/{id}")
                .method("GET")
                .scanMessage("Missing authentication")
                .scanTimestamp(scanTimestamp)
                .build();
        SecscanFlaggedEndpoint ep2 = SecscanFlaggedEndpoint.builder()
                .systemComponentId(componentA.getId())
                .path("/api/admin/config")
                .method("POST")
                .scanMessage("No authorization check")
                .scanTimestamp(scanTimestamp)
                .build();

        repository.saveAll(List.of(ep1, ep2));
        flushAndClear();

        SecscanFlaggedEndpoint found1 = entityManager.find(SecscanFlaggedEndpoint.class, ep1.getId());
        assertThat(found1).isNotNull();
        assertThat(found1.getSystemComponentId()).isEqualTo(componentA.getId());
        assertThat(found1.getPath()).isEqualTo("/api/users/{id}");
        assertThat(found1.getMethod()).isEqualTo("GET");
        assertThat(found1.getScanMessage()).isEqualTo("Missing authentication");
        assertThat(found1.getScanTimestamp().toInstant()).isEqualTo(scanTimestamp.toInstant());

        SecscanFlaggedEndpoint found2 = entityManager.find(SecscanFlaggedEndpoint.class, ep2.getId());
        assertThat(found2).isNotNull();
        assertThat(found2.getSystemComponentId()).isEqualTo(componentA.getId());
        assertThat(found2.getPath()).isEqualTo("/api/admin/config");
        assertThat(found2.getMethod()).isEqualTo("POST");
        assertThat(found2.getScanMessage()).isEqualTo("No authorization check");
        assertThat(found2.getScanTimestamp().toInstant()).isEqualTo(scanTimestamp.toInstant());
    }

    @Test
    void saveAll_withNullMessage() {
        SystemComponent componentA = persistSystemComponent(COMPONENT_A);
        ZonedDateTime scanTimestamp = ZonedDateTime.now().truncatedTo(ChronoUnit.MICROS);
        SecscanFlaggedEndpoint ep = SecscanFlaggedEndpoint.builder()
                .systemComponentId(componentA.getId())
                .path("/api/data")
                .method("DELETE")
                .scanTimestamp(scanTimestamp)
                .build();

        repository.saveAll(List.of(ep));
        flushAndClear();

        SecscanFlaggedEndpoint found = entityManager.find(SecscanFlaggedEndpoint.class, ep.getId());
        assertThat(found).isNotNull();
        assertThat(found.getSystemComponentId()).isEqualTo(componentA.getId());
        assertThat(found.getPath()).isEqualTo("/api/data");
        assertThat(found.getMethod()).isEqualTo("DELETE");
        assertThat(found.getScanMessage()).isNull();
    }

    @Test
    void deleteBySystemComponentId() {
        SystemComponent componentA = persistSystemComponent(COMPONENT_A);
        SystemComponent componentB = persistSystemComponent(COMPONENT_B);
        SecscanFlaggedEndpoint epA1 = SecscanFlaggedEndpoint.builder()
                .systemComponentId(componentA.getId())
                .path("/api/users")
                .method("GET")
                .scanMessage("flagged")
                .scanTimestamp(ZonedDateTime.now().truncatedTo(ChronoUnit.MICROS))
                .build();
        SecscanFlaggedEndpoint epA2 = SecscanFlaggedEndpoint.builder()
                .systemComponentId(componentA.getId())
                .path("/api/users")
                .method("POST")
                .scanMessage("flagged")
                .scanTimestamp(ZonedDateTime.now().truncatedTo(ChronoUnit.MICROS))
                .build();
        SecscanFlaggedEndpoint epB1 = SecscanFlaggedEndpoint.builder()
                .systemComponentId(componentB.getId())
                .path("/api/orders")
                .method("GET")
                .scanMessage("flagged")
                .scanTimestamp(ZonedDateTime.now().truncatedTo(ChronoUnit.MICROS))
                .build();
        entityManager.persist(epA1);
        entityManager.persist(epA2);
        entityManager.persist(epB1);
        flushAndClear();

        int deletedCount = repository.deleteBySystemComponentId(componentA.getId());

        assertThat(deletedCount).isEqualTo(2);
        assertThat(entityManager.find(SecscanFlaggedEndpoint.class, epA1.getId())).isNull();
        assertThat(entityManager.find(SecscanFlaggedEndpoint.class, epA2.getId())).isNull();
        assertThat(entityManager.find(SecscanFlaggedEndpoint.class, epB1.getId())).isNotNull();
    }

    @Test
    void deleteBySystemComponentId_noMatch() {
        SystemComponent componentA = persistSystemComponent(COMPONENT_A);
        SystemComponent componentB = persistSystemComponent(COMPONENT_B);
        SecscanFlaggedEndpoint epB1 = SecscanFlaggedEndpoint.builder()
                .systemComponentId(componentB.getId())
                .path("/api/orders")
                .method("GET")
                .scanMessage("flagged")
                .scanTimestamp(ZonedDateTime.now().truncatedTo(ChronoUnit.MICROS))
                .build();
        entityManager.persist(epB1);
        flushAndClear();

        int deletedCount = repository.deleteBySystemComponentId(componentA.getId());

        assertThat(deletedCount).isZero();
        assertThat(entityManager.find(SecscanFlaggedEndpoint.class, epB1.getId())).isNotNull();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
