package ch.admin.bit.jeap.governance.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Repository to get the current {@link jakarta.persistence.Entity} and create new objects for this model
 */
@Component
public class EntityFactory {

    /**
     * @param name the name of the system
     * @param aliases the set of aliases
     * Create a new system but do not add it to the model or DB yet
     */
    public System createNewSystem(String name, Set<String> aliases) {
        return System.builder()
                //If no ID given yet, choose random
                .id(UUID.randomUUID())
                .name(name)
                .aliases(aliases)
                .systemComponents(List.of())
                //As no rules evaluated so far state is OK
                .state(State.OK)
                .build();
    }

    /**
     * Create a new system component but do not add it to the model or DB yet
     * @param name the name of the system component
     * @param type the component type
     */
    public SystemComponent createNewSystemComponent(String name, ComponentType type) {
        return SystemComponent.builder()
                //If no ID given yet, choose random
                .id(UUID.randomUUID())
                .name(name)
                //As no rules evaluated so far state is OK
                .state(State.OK)
                .type(type)
                .build();
    }
}
