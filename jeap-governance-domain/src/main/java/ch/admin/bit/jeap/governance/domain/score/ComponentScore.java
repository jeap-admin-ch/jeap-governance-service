package ch.admin.bit.jeap.governance.domain.score;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;

@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED) // for jpa
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ComponentScore {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "component_score_seq")
    @SequenceGenerator(
            name = "component_score_seq",
            sequenceName = "component_score_id_seq"
    )
    @EqualsAndHashCode.Include
    @Getter
    private Long id;

    @Getter
    private int score;

    @ManyToOne
    @Getter
    @Setter(AccessLevel.PACKAGE)
    @ToString.Exclude
    private SystemComponent systemComponent;

    @Column(nullable = false)
    private LocalDate day;

    @Getter
    private ZonedDateTime createdAt;

    private ComponentScore(SystemComponent systemComponent, int score, LocalDate day, ZonedDateTime createdAt) {
        this.systemComponent = systemComponent;
        this.score = score;
        this.day = day;
        this.createdAt = createdAt;
    }

    @Builder
    private static ComponentScore build(@NonNull SystemComponent systemComponent, int score, @NonNull LocalDate day) {
        return new ComponentScore(systemComponent, score, day, ZonedDateTime.now());
    }
}
