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
@Entity(name = "ReactionGraph")
@Table(name = "ar_reaction_graph")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor
public class ReactionGraph {
    @Id
    @NonNull
    @EqualsAndHashCode.Include
    @Getter
    private UUID id;

    @NonNull
    @Getter
    private ZonedDateTime lastModifiedAt;

    @ManyToOne
    @Getter
    private SystemComponent systemComponent;

    @Getter
    private ZonedDateTime createdAt;

    public void updateLastModifiedAt(ZonedDateTime lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }

    @Builder
    private static ReactionGraph build(UUID id, ZonedDateTime lastModifiedAt, SystemComponent systemComponent, ZonedDateTime createdAt) {
        return new ReactionGraph(id, lastModifiedAt, systemComponent, createdAt == null ? ZonedDateTime.now() : createdAt);
    }
}
