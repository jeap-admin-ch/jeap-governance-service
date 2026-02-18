package ch.admin.bit.jeap.governance.prometheus.persistence;

import ch.admin.bit.jeap.governance.prometheus.domain.PromQueryType;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeries;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeriesQueryRepository;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeriesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
class PromTimeSeriesRepositoryImpl implements PromTimeSeriesRepository, PromTimeSeriesQueryRepository {

    private final JpaPromTimeSeriesRepository jpaRepository;

    public void saveAll(List<PromTimeSeries> timeSeriesList) {
        jpaRepository.saveAll(timeSeriesList);
    }

    @Override
    public int deleteBySystemComponentId(long systemComponentId) {
        return jpaRepository.deleteBySystemComponentId(systemComponentId);
    }

    @Override
    public List<PromTimeSeries> findBy(PromQueryType promQueryType, long systemComponentId) {
        return jpaRepository.findByPrometheusQueryTypeAndSystemComponentId(promQueryType, systemComponentId);
    }

    @Override
    public boolean anyTimeSeriesExistsBy(long systemComponentId) {
        return jpaRepository.existsBySystemComponentId(systemComponentId);
    }

}
