package ch.admin.bit.jeap.governance.archrepo.domain;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED) // for jpa
@Entity(name = "DatabaseSchemaVersion")
@Table(name = "ar_database_schema_version")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor
public class DatabaseSchemaVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ar_database_schema_version_seq")
    @SequenceGenerator(
            name = "ar_database_schema_version_seq",
            sequenceName = "ar_database_schema_version_id_seq"
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

    private DatabaseSchemaVersion(String version, SystemComponent systemComponent, ZonedDateTime zonedDateTime) {
        this.version = version;
        this.systemComponent = systemComponent;
        this.createdAt = zonedDateTime;
    }

    @Builder
    private static DatabaseSchemaVersion build(String version, SystemComponent systemComponent, ZonedDateTime createdAt) {
        return new DatabaseSchemaVersion(version, systemComponent, createdAt == null ? ZonedDateTime.now() : createdAt);
    }
}
