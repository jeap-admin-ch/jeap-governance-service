package ch.admin.bit.jeap.governance.prometheus.domain;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.ZonedDateTime;

@Entity
@Table(name = "prom_time_series")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // for jpa
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PromTimeSeries {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "prom_time_series_seq_gen")
    @SequenceGenerator(
            name = "prom_time_series_seq_gen",
            sequenceName = "prom_time_series_seq"
    )
    @EqualsAndHashCode.Include
    @Column(nullable = false)
    private Long id;

    @NonNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PromQueryType prometheusQueryType;

    @NonNull
    @Column(nullable = false)
    private ZonedDateTime queryTimestamp;

    @NonNull
    @Column(nullable = false)
    private String systemComponentName;

    @JdbcTypeCode(SqlTypes.JSON)
    @NonNull
    @Column(columnDefinition = "jsonb", nullable = false)
    private PromTimeSeriesSample sample;

}
