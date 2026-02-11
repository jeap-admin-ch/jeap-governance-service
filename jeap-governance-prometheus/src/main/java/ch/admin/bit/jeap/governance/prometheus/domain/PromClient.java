package ch.admin.bit.jeap.governance.prometheus.domain;

import ch.admin.bit.jeap.governance.domain.GovernanceServiceEnvironment;

import java.util.List;

public interface PromClient {

    List<PromTimeSeriesSample> query(PromQueryType queryType, GovernanceServiceEnvironment environment, String... args);

}
