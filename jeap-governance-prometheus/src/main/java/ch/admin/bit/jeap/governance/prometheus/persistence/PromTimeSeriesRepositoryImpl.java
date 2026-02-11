package ch.admin.bit.jeap.governance.prometheus.persistence;

import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeries;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeriesQueryRepository;
import ch.admin.bit.jeap.governance.prometheus.domain.PromQueryType;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeriesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
class PromTimeSeriesRepositoryImpl implements PromTimeSeriesRepository, PromTimeSeriesQueryRepository {

    private final JpaPromTimeSeriesRepository jpaRepository;

    public Iterable<PromTimeSeries> saveAll(List<PromTimeSeries> timeSeriesList) {
        return jpaRepository.saveAll(timeSeriesList);
    }

    @Override
    public int deleteBy(String systemComponent) {
        return jpaRepository.deleteBy(systemComponent);
    }

    @Override
    public List<PromTimeSeries> findBy(PromQueryType promQueryType, String systemComponent) {
        return jpaRepository.findByPrometheusQueryTypeAndSystemComponentName(promQueryType, systemComponent);
    }

}
