package ch.admin.bit.jeap.governance.reactionobserver.persistence;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import lombok.experimental.UtilityClass;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Set;

@UtilityClass
public class PersistenceTestUtility {

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

    static System createAndPersistSystemWithSystemComponents(TestEntityManager entityManager, String systemName, SystemComponent... systemComponents) {
        System system = System.builder()
                .name(systemName)
                .systemComponents(systemComponents == null ? List.of() : List.of(systemComponents))
                .aliases(Set.of("a " + systemName))
                .build();

        System savedSystem = entityManager.persist(system);
        entityManager.flush();
        return savedSystem;
    }

}
