package ch.admin.bit.jeap.governance.prometheus.persistence;

import ch.admin.bit.jeap.governance.prometheus.domain.PromQueryType;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeries;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface JpaPromTimeSeriesRepository extends CrudRepository<PromTimeSeries, Long> {

    List<PromTimeSeries> findByPrometheusQueryTypeAndSystemComponentName(PromQueryType promQueryType, String systemComponentName);

    @Modifying
    @Query("DELETE FROM PromTimeSeries p WHERE p.systemComponentName = ?1")
    int deleteBy(String systemComponentName);

}
