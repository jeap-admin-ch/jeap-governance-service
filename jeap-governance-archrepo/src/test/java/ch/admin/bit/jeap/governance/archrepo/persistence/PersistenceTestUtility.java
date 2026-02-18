package ch.admin.bit.jeap.governance.archrepo.persistence;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import lombok.experimental.UtilityClass;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Set;

@UtilityClass
public class PersistenceTestUtility {

    static TwoSystemComponents createAndPersistSystemWithTwoSystemComponents(String systemName, TestEntityManager entityManager) {
        SystemComponent systemComponent1 = SystemComponent.builder()
                .name(systemName + "-Test Component1")
                .type(ComponentType.BACKEND_SERVICE)
                .build();
        SystemComponent systemComponent2 = SystemComponent.builder()
                .name(systemName + "-Test Component2")
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
                .name(systemName + "-Test Component")
                .type(ComponentType.BACKEND_SERVICE)
                .build();
        createAndPersistSystemWithSystemComponents(entityManager, systemName, systemComponent);
        return systemComponent;
    }

    static void createAndPersistSystemWithSystemComponents(TestEntityManager entityManager, String systemName, SystemComponent... systemComponents) {
        System system = System.builder()
                .name(systemName)
                .systemComponents(systemComponents == null ? List.of() : List.of(systemComponents))
                .aliases(Set.of("a " + systemName))
                .build();

        entityManager.persist(system);
        entityManager.flush();
    }

    public record TwoSystemComponents(SystemComponent first, SystemComponent second) {
    }
}
