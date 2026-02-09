package ch.admin.bit.jeap.governance.deploymentlog.domain;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED) // for jpa
@Entity(name = "DeploymentLogComponentVersion")
@Table(name = "dl_component_version")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor
public class DeploymentLogComponentVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dl_component_version_seq")
    @SequenceGenerator(
            name = "dl_component_version_seq",
            sequenceName = "dl_component_version_id_seq"
    )
    @EqualsAndHashCode.Include
    @Getter
    private Long id;

    @NonNull
    @Getter
    String version;

    @ManyToOne
    @Getter
    private SystemComponent systemComponent;

    @Getter
    private ZonedDateTime createdAt;

    private DeploymentLogComponentVersion(String version, SystemComponent systemComponent, ZonedDateTime zonedDateTime) {
        this.version = version;
        this.systemComponent = systemComponent;
        this.createdAt = zonedDateTime;
    }

    @Builder
    private static DeploymentLogComponentVersion build(UUID id, String version, SystemComponent systemComponent, ZonedDateTime createdAt) {
        return new DeploymentLogComponentVersion(version, systemComponent, createdAt == null ? ZonedDateTime.now() : createdAt);
    }
}
