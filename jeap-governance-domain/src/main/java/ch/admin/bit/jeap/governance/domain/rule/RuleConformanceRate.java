package ch.admin.bit.jeap.governance.domain.rule;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;

@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED) // for jpa
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RuleConformanceRate {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rule_conformance_rate_seq")
    @SequenceGenerator(
            name = "rule_conformance_rate_seq",
            sequenceName = "rule_conformance_rate_id_seq"
    )
    @EqualsAndHashCode.Include
    @Getter
    private Long id;

    @Getter
    private String ruleId;

    @Getter
    private int conformanceRate;

    @Column(nullable = false)
    private LocalDate day;

    @Getter
    private ZonedDateTime createdAt;

    private RuleConformanceRate(String ruleId, int conformanceRate, LocalDate day, ZonedDateTime createdAt) {
        this.ruleId = ruleId;
        this.conformanceRate = conformanceRate;
        this.day = day;
        this.createdAt = createdAt;
    }

    @Builder
    private static RuleConformanceRate build(@NonNull String ruleId, int conformanceRate, @NonNull LocalDate day) {
        return new RuleConformanceRate(ruleId, conformanceRate, day, ZonedDateTime.now());
    }
}
