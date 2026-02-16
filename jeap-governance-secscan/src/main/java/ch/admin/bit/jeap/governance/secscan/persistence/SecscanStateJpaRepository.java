package ch.admin.bit.jeap.governance.secscan.persistence;

import ch.admin.bit.jeap.governance.secscan.domain.SecscanState;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

interface SecscanStateJpaRepository extends CrudRepository<SecscanState, Long> {

    Optional<SecscanState> findBySystemComponentId(long systemComponentId);

    @Modifying
    @Query("DELETE FROM SecscanState s WHERE s.systemComponentId = ?1")
    int deleteBySystemComponentId(long systemComponentId);

}
