package ch.admin.bit.jeap.governance.archrepo.persistence;

import ch.admin.bit.jeap.governance.archrepo.domain.ReactionGraph;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaReactionGraphRepository extends CrudRepository<ReactionGraph, UUID> {

    Optional<ReactionGraph> findBySystemComponentName(String componentName);

    Optional<ReactionGraph> findBySystemComponentId(UUID componentId);
}
