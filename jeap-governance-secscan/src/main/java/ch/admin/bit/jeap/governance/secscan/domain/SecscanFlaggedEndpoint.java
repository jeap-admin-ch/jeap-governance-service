package ch.admin.bit.jeap.governance.secscan.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;


@Entity
@Table(name = "secscan_flagged_endpoint")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // for jpa
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SecscanFlaggedEndpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "secscan_flagged_endpoint_seq_gen")
    @SequenceGenerator(
            name = "secscan_flagged_endpoint_seq_gen",
            sequenceName = "secscan_flagged_endpoint_seq"
    )
    @EqualsAndHashCode.Include
    @Column(nullable = false)
    private Long id;

    @Column(nullable = false)
    private long systemComponentId;

    @NonNull
    @Column(nullable = false)
    private String path;

    @NonNull
    @Column(nullable = false)
    private String method;

    @Column
    private String scanMessage;

    @NonNull
    @Column(nullable = false)
    private ZonedDateTime scanTimestamp;

}
