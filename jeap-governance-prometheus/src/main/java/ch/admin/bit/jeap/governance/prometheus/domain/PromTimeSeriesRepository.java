package ch.admin.bit.jeap.governance.prometheus.domain;

import java.util.List;

public interface PromTimeSeriesRepository {

    void saveAll(List<PromTimeSeries> promTimeSeriesList);

    int deleteBySystemComponentId(long systemComponentId);

}
