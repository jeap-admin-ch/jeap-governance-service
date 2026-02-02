package ch.admin.bit.jeap.governance.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.*;

@NoArgsConstructor(access = AccessLevel.PROTECTED) // for jpa
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity(name = "System")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class System {
    @Getter
    @Id
    @NonNull
    @EqualsAndHashCode.Include
    private UUID id;

    @NonNull
    @Getter
    private String name;

    @Getter
    @ElementCollection(fetch = FetchType.LAZY)
    private Set<String> aliases;

    @OneToMany(mappedBy = "system",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true)
    @ToString.Exclude
    private List<SystemComponent> systemComponents;

    @Enumerated(EnumType.STRING)
    @NonNull
    @Getter
    @Setter
    private State state;

    @NonNull
    @Getter
    private ZonedDateTime createdAt;

    @Builder
    private static System build(UUID id, String name, Set<String> aliases, List<SystemComponent> systemComponents, State state, ZonedDateTime createdAt) {
        //This list might be read only, we need a "normal" list
        List<SystemComponent> modifiableList = new ArrayList<>(systemComponents);
        System system = new System(id, name, copyAliases(aliases), modifiableList, state, createdAt == null ? ZonedDateTime.now() : createdAt);
        //Set the system for each service
        systemComponents.forEach(x -> x.setSystem(system));
        return system;
    }

    private static Set<String> copyAliases(Set<String> aliases) {
        // Must be a mutable Set for JPA
        return aliases != null ? new HashSet<>(aliases) : new HashSet<>();
    }

    public List<SystemComponent> getSystemComponents() {
        return Collections.unmodifiableList(systemComponents);
    }

    public void setAliases(Set<String> aliases) {
        this.aliases = copyAliases(aliases);
    }

    public void addSystemComponent(SystemComponent systemComponent) {
        systemComponent.setSystem(this);
        systemComponents.add(systemComponent);
    }

    public void deleteSystemComponent(SystemComponent systemComponent) {
        systemComponents.removeIf(s -> s.getId().equals(systemComponent.getId()));
    }

    public Optional<SystemComponent> getSystemComponentByName(String name) {
        return systemComponents.stream()
                .filter(component -> name.equalsIgnoreCase(component.getName()))
                .findFirst();
    }
}
