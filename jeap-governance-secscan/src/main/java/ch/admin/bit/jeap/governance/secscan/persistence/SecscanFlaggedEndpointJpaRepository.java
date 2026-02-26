package ch.admin.bit.jeap.governance.secscan.persistence;

import ch.admin.bit.jeap.governance.secscan.domain.SecscanFlaggedEndpoint;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

interface SecscanFlaggedEndpointJpaRepository extends CrudRepository<SecscanFlaggedEndpoint, Long> {

    List<SecscanFlaggedEndpoint> findBySystemComponentId(long systemComponentId);

    @Modifying
    @Query("DELETE FROM SecscanFlaggedEndpoint fe WHERE fe.systemComponentId = ?1")
    int deleteBySystemComponentId(long systemComponentId);

}
