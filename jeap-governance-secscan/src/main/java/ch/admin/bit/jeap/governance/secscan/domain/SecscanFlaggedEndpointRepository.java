package ch.admin.bit.jeap.governance.secscan.domain;

import java.util.List;

public interface SecscanFlaggedEndpointRepository {

    List<SecscanFlaggedEndpoint> saveAll(List<SecscanFlaggedEndpoint> flaggedEndpoints);

    int deleteBySystemComponentId(long systemComponentId);

}
