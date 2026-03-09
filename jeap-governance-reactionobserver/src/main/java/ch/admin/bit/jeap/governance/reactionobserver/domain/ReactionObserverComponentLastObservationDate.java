package ch.admin.bit.jeap.governance.reactionobserver.domain;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;

@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED) // for jpa
@Entity(name = "ReactionObserverComponentLastObservationDate")
@Table(name = "ro_component_last_observation_date")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor
public class ReactionObserverComponentLastObservationDate {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ro_component_last_observation_date_seq")
    @SequenceGenerator(
            name = "ro_component_last_observation_date_seq",
            sequenceName = "ro_component_last_observation_date_id_seq"
    )
    @EqualsAndHashCode.Include
    @Getter
    private Long id;

    @NonNull
    @Getter
    LocalDate lastObservationDate;

    @ManyToOne
    @Getter
    private SystemComponent systemComponent;

    @Getter
    private ZonedDateTime createdAt;

    private ReactionObserverComponentLastObservationDate(SystemComponent systemComponent, @NonNull LocalDate lastObservationDate, ZonedDateTime zonedDateTime) {
        this.lastObservationDate = lastObservationDate;
        this.systemComponent = systemComponent;
        this.createdAt = zonedDateTime;
    }

    @Builder
    private static ReactionObserverComponentLastObservationDate build(LocalDate lastObservationDate, SystemComponent systemComponent, ZonedDateTime createdAt) {
        return new ReactionObserverComponentLastObservationDate(systemComponent, lastObservationDate, createdAt == null ? ZonedDateTime.now() : createdAt);
    }
}
