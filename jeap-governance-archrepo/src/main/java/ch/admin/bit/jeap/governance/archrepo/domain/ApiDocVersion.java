package ch.admin.bit.jeap.governance.archrepo.domain;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
    @NonNull
    @EqualsAndHashCode.Include
    @Getter
    private UUID id;

    @NonNull
    @Getter
    String version;

    @ManyToOne
    @Getter
    private SystemComponent systemComponent;

    @Getter
    private ZonedDateTime createdAt;

    @Builder
    private static ApiDocVersion build(UUID id, String version, SystemComponent systemComponent, ZonedDateTime createdAt) {
        return new ApiDocVersion(id, version, systemComponent, createdAt == null ? ZonedDateTime.now() : createdAt);
    }
}
