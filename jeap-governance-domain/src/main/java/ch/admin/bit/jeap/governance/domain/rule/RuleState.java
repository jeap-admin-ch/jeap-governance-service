package ch.admin.bit.jeap.governance.domain.rule;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED) // for jpa
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RuleState {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rule_state_rate_seq")
    @SequenceGenerator(
            name = "rule_state_rate_seq",
            sequenceName = "rule_state_rate_id_seq"
    )
    @EqualsAndHashCode.Include
    @Getter
    private Long id;

    @Getter
    private String ruleId;

    @ManyToOne
    @Getter
    @Setter(AccessLevel.PACKAGE)
    @ToString.Exclude
    private SystemComponent systemComponent;

    @Enumerated(EnumType.STRING)
    @Getter
    private State state;

    @Getter
    private String stateComment;

    @Getter
    private ZonedDateTime createdAt;

    @Getter
    private ZonedDateTime modifiedAt;

    private RuleState(String ruleId, SystemComponent systemComponent, State state, String stateComment, ZonedDateTime createdAt, ZonedDateTime modifiedAt) {
        this.ruleId = ruleId;
        this.systemComponent = systemComponent;
        this.state = state;
        this.stateComment = stateComment;
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
    }

    @Builder
    private static RuleState build(@NonNull RuleId ruleId, @NonNull SystemComponent systemComponent, @NonNull State state, String ruleStateComment) {
        var now = ZonedDateTime.now();
        return new RuleState(ruleId.id(), systemComponent, state, ruleStateComment, now, now);
    }

    public static RuleState createWithTimestamps(@NonNull RuleId ruleId, @NonNull SystemComponent systemComponent, @NonNull State state,
                                                 @NonNull ZonedDateTime createdAt, @NonNull ZonedDateTime modifiedAt) {
        return new RuleState(ruleId.id(), systemComponent, state, null, createdAt, modifiedAt);
    }

    public void modify(State state, String stateComment) {
        this.state = state;
        this.stateComment = stateComment;
        this.modifiedAt = ZonedDateTime.now();
    }
}
