package ch.admin.bit.jeap.governance.secscan.persistence;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Set;

@Testcontainers
public abstract class PostgresTestContainerBase {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer(
                DockerImageName.parse("postgres:17-alpine").
                        asCompatibleSubstituteFor("postgres:17-alpine"));

    @Autowired
    protected TestEntityManager entityManager;

    protected SystemComponent persistSystemComponent(String name) {
        SystemComponent component = SystemComponent.builder()
                .name(name)
                .type(ComponentType.BACKEND_SERVICE)
                .build();
        System system = System.builder()
                .name("system-" + name)
                .aliases(Set.of())
                .systemComponents(List.of(component))
                .build();
        entityManager.persistAndFlush(system);
        return system.getSystemComponents().getFirst();
    }

}
