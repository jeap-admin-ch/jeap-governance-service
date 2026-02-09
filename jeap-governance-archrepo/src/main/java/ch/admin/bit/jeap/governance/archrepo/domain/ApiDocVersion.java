package ch.admin.bit.jeap.governance.archrepo.domain;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED) // for jpa
@Entity(name = "ApiDocVersion")
@Table(name = "ar_api_doc_version")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor
public class ApiDocVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ar_api_doc_version_seq")
    @SequenceGenerator(
            name = "ar_api_doc_version_seq",
            sequenceName = "ar_api_doc_version_id_seq"
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

    private ApiDocVersion(String version, SystemComponent systemComponent, ZonedDateTime zonedDateTime) {
        this.version = version;
        this.systemComponent = systemComponent;
        this.createdAt = zonedDateTime;
    }

    @Builder
    private static ApiDocVersion build(UUID id, String version, SystemComponent systemComponent, ZonedDateTime createdAt) {
        return new ApiDocVersion(version, systemComponent, createdAt == null ? ZonedDateTime.now() : createdAt);
    }
}
