package ch.admin.bit.jeap.governance.archrepo.persistence;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.State;
import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import lombok.experimental.UtilityClass;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@UtilityClass
public class PersistenceTestUtility {

    static TwoSystemComponents createAndPersistSystemWithTwoSystemComponents(String systemName, TestEntityManager entityManager) {
        SystemComponent systemComponent1 = SystemComponent.builder()
                .id(UUID.randomUUID())
                .name(systemName + "-Test Component1")
                .state(State.OK)
                .type(ComponentType.BACKEND_SERVICE)
                .build();
        SystemComponent systemComponent2 = SystemComponent.builder()
                .id(UUID.randomUUID())
                .name(systemName + "-Test Component2")
                .state(State.OK)
                .type(ComponentType.BACKEND_SERVICE)
                .build();
        createAndPersistSystemWithSystemComponents(entityManager, systemName, systemComponent1, systemComponent2);
        return new TwoSystemComponents(systemComponent1, systemComponent2);
    }

    static TwoSystemComponents createAndPersistSystemWithTwoSystemComponents(TestEntityManager entityManager) {
        return createAndPersistSystemWithTwoSystemComponents("Test System", entityManager);
    }

    static SystemComponent createAndPersistSystemWithOneSystemComponent(TestEntityManager entityManager) {
        return createAndPersistSystemWithOneSystemComponent("Test Component", entityManager);
    }

    static SystemComponent createAndPersistSystemWithOneSystemComponent(String systemName, TestEntityManager entityManager) {
        SystemComponent systemComponent = SystemComponent.builder()
                .id(UUID.randomUUID())
                .name(systemName + "-Test Component")
                .state(State.OK)
                .type(ComponentType.BACKEND_SERVICE)
                .build();
        createAndPersistSystemWithSystemComponents(entityManager, systemName, systemComponent);
        return systemComponent;
    }

    static System createAndPersistSystemWithSystemComponents(TestEntityManager entityManager, String systemName, SystemComponent... systemComponents) {
        System system = System.builder()
                .id(UUID.randomUUID())
                .name(systemName)
                .systemComponents(systemComponents == null ? List.of() : List.of(systemComponents))
                .state(State.OK)
                .aliases(Set.of("a " + systemName))
                .build();

        entityManager.persist(system);
        entityManager.flush();
        return system;
    }

    public record TwoSystemComponents(SystemComponent first, SystemComponent second) {
    }
}
