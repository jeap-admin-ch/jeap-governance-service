package ch.admin.bit.jeap.governance.prometheus.domain;

import java.util.List;

public interface PromTimeSeriesRepository {

    Iterable<PromTimeSeries> saveAll(List<PromTimeSeries> promTimeSeriesList);

    int deleteBy(String systemComponentName);

}
