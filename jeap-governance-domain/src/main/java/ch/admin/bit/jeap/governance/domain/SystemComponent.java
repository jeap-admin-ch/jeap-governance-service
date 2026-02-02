package ch.admin.bit.jeap.governance.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED) // for jpa
@Entity(name = "SystemComponent")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SystemComponent {
    @Id
    @NonNull
    @EqualsAndHashCode.Include
    @Getter
    private UUID id;

    @NonNull
    @Getter
    private String name;

    @Enumerated(EnumType.STRING)
    @NonNull
    @Getter
    @Setter
    private State state;

    @ManyToOne
    @Getter
    @Setter(AccessLevel.PACKAGE)
    @ToString.Exclude
    private System system;

    @Enumerated(EnumType.STRING)
    @NonNull
    @Getter
    private ComponentType type;

    @NonNull
    @Getter
    private ZonedDateTime createdAt;

    private SystemComponent(UUID id, String name, State state, ComponentType type, ZonedDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.state = state;
        this.type = type;
        this.createdAt = createdAt;
    }

    @Builder
    private static SystemComponent build(UUID id, String name, State state, ComponentType type, ZonedDateTime createdAt) {
        // System is set afterward when a service is added to a system
        return new SystemComponent(id, name, state, type, createdAt == null ? ZonedDateTime.now() : createdAt);
    }

    public void update(ComponentType type) {
        this.type = type;
    }
}
