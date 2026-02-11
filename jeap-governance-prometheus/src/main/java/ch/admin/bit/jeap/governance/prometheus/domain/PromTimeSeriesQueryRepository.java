package ch.admin.bit.jeap.governance.prometheus.domain;

import java.util.List;

public interface PromTimeSeriesQueryRepository {

    List<PromTimeSeries> findBy(PromQueryType queryType, String systemComponent);

    // Additional methods for querying time series to be implemented as needed by rule implementations JEAP-6590, JEAP-6588, ...

}
