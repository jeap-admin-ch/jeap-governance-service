package ch.admin.bit.jeap.governance.secscan.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;


@Entity
@Table(name = "secscan_state")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // for jpa
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SecscanState {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "secscan_state_seq_gen")
    @SequenceGenerator(
            name = "secscan_state_seq_gen",
            sequenceName = "secscan_state_seq"
    )
    @EqualsAndHashCode.Include
    @Column(nullable = false)
    private Long id;

    @Column(nullable = false)
    private long systemComponentId;

    @Column
    private String scanMessage;

    @NonNull
    @Column(nullable = false)
    private ZonedDateTime scanTimestamp;

}
