package ch.admin.bit.jeap.governance.domain.rule;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import lombok.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;

/**
 * Persisted daily conformance rate percentage for a specific rule within a single system.
 * The conformance rate is the percentage of system components where the rule state is OK or DISABLED.
 */
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED) // for jpa
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SystemRuleConformanceRate {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "system_rule_conformance_rate_seq")
    @SequenceGenerator(
            name = "system_rule_conformance_rate_seq",
            sequenceName = "system_rule_conformance_rate_id_seq"
    )
    @EqualsAndHashCode.Include
    @Getter
    private Long id;

    @Getter
    private long systemId;

    @Getter
    private String ruleId;

    @Getter
    private int conformanceRate;

    @Column(nullable = false)
    @Getter
    private LocalDate day;

    @Getter
    private ZonedDateTime createdAt;

    private SystemRuleConformanceRate(long systemId, String ruleId, int conformanceRate, LocalDate day, ZonedDateTime createdAt) {
        this.systemId = systemId;
        this.ruleId = ruleId;
        this.conformanceRate = conformanceRate;
        this.day = day;
        this.createdAt = createdAt;
    }

    @Builder
    private static SystemRuleConformanceRate build(long systemId, @NonNull String ruleId, int conformanceRate, @NonNull LocalDate day, ZonedDateTime createdAt) {
        return new SystemRuleConformanceRate(systemId, ruleId, conformanceRate, day, createdAt == null ? ZonedDateTime.now() : createdAt);
    }
}
