package ch.admin.bit.jeap.governance.prometheus.persistence;

import ch.admin.bit.jeap.governance.prometheus.domain.PromQueryType;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeries;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaPromTimeSeriesRepository extends JpaRepository<PromTimeSeries, Long> {

    List<PromTimeSeries> findByPrometheusQueryTypeAndSystemComponentId(PromQueryType promQueryType, long systemComponentId);

    boolean existsBySystemComponentId(long systemComponentId);

    @Modifying
    int deleteBySystemComponentId(long systemComponentId);
}
