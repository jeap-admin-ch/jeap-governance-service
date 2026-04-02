package ch.admin.bit.jeap.governance.secscan.persistence;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Set;

public abstract class PostgresTestContainerBase {

    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
                DockerImageName.parse("postgres:17-alpine").
                        asCompatibleSubstituteFor("postgres:17-alpine"));
    static {
        postgres.start();
    }

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
