package ch.admin.bit.jeap.governance.archrepo.domain;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED) // for jpa
@Entity(name = "ReactionGraph")
@Table(name = "ar_reaction_graph")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor
public class ReactionGraph {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ar_reaction_graph_seq")
    @SequenceGenerator(
            name = "ar_reaction_graph_seq",
            sequenceName = "ar_reaction_graph_id_seq"
    )
    @EqualsAndHashCode.Include
    @Getter
    private Long id;

    @NonNull
    @Getter
    private ZonedDateTime lastModifiedAt;

    @ManyToOne
    @Getter
    private SystemComponent systemComponent;

    @Getter
    private ZonedDateTime createdAt;

    private ReactionGraph(ZonedDateTime lastModifiedAt, SystemComponent systemComponent, ZonedDateTime zonedDateTime) {
        this.lastModifiedAt = lastModifiedAt;
        this.systemComponent = systemComponent;
        this.createdAt = zonedDateTime;
    }

    @Builder
    private static ReactionGraph build(ZonedDateTime lastModifiedAt, SystemComponent systemComponent, ZonedDateTime createdAt) {
        return new ReactionGraph(lastModifiedAt, systemComponent, createdAt == null ? ZonedDateTime.now() : createdAt);
    }
}
