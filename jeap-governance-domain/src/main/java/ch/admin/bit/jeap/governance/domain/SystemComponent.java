package ch.admin.bit.jeap.governance.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

@ToString(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED) // for jpa
@Entity(name = "SystemComponent")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SystemComponent {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "system_component_seq")
    @SequenceGenerator(
            name = "system_component_seq",
            sequenceName = "system_component_id_seq"
    )
    @EqualsAndHashCode.Include
    @ToString.Include
    @Getter
    private Long id;

    @ToString.Include
    @NonNull
    @Getter
    private String name;

    @ManyToOne
    @Getter
    @Setter(AccessLevel.PACKAGE)
    private System system;

    @Enumerated(EnumType.STRING)
    @NonNull
    @Getter
    private ComponentType type;

    @NonNull
    @Getter
    private ZonedDateTime createdAt;

    private SystemComponent(@NonNull String name, @NonNull ComponentType type, @NonNull ZonedDateTime createdAt) {
        this.name = name;
        this.type = type;
        this.createdAt = createdAt;
    }

    @Builder
    private static SystemComponent build(@NonNull String name, @NonNull ComponentType type, ZonedDateTime createdAt) {
        // System is set afterward when a service is added to a system
        return new SystemComponent(name, type, createdAt == null ? ZonedDateTime.now() : createdAt);
    }

    public void update(ComponentType type) {
        this.type = type;
    }
}
