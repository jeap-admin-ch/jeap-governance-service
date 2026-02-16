package ch.admin.bit.jeap.governance.secscan.domain;

import java.util.Optional;

public interface SecscanStateRepository {

    SecscanState save(SecscanState secscanState);

    Optional<SecscanState> findBySystemComponentId(long systemComponentId);

    int deleteBySystemComponentId(long systemComponentId);

}
