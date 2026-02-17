package ch.admin.bit.jeap.governance.domain.score;

import ch.admin.bit.jeap.governance.domain.System;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;

@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED) // for jpa
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SystemScore {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "system_score_seq")
    @SequenceGenerator(
            name = "system_score_seq",
            sequenceName = "system_score_id_seq"
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
    private ch.admin.bit.jeap.governance.domain.System system;

    @Getter
    @Column(nullable = false)
    private LocalDate day;

    @Getter
    private ZonedDateTime createdAt;

    private SystemScore(ch.admin.bit.jeap.governance.domain.System system, int score, LocalDate day, ZonedDateTime createdAt) {
        this.system = system;
        this.score = score;
        this.day = day;
        this.createdAt = createdAt;
    }

    @Builder
    private static SystemScore build(@NonNull System system, int score, @NonNull LocalDate day) {
        return new SystemScore(system, score, day, ZonedDateTime.now());
    }
}
