package ch.admin.bit.jeap.governance.archrepo.persistence;

import ch.admin.bit.jeap.governance.archrepo.domain.ReactionGraph;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface JpaReactionGraphRepository extends CrudRepository<ReactionGraph, Long> {

    Optional<ReactionGraph> findBySystemComponentName(String componentName);

    Optional<ReactionGraph> findBySystemComponentId(Long componentId);
}
