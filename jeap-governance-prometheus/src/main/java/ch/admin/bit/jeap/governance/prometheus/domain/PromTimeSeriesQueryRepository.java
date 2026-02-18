package ch.admin.bit.jeap.governance.prometheus.domain;

import java.util.List;

public interface PromTimeSeriesQueryRepository {

    List<PromTimeSeries> findBy(PromQueryType queryType, long systemComponentId);

    boolean anyTimeSeriesExistsBy(long systemComponentId);
}
