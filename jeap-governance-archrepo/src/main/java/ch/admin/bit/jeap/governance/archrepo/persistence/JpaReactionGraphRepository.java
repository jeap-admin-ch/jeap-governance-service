package ch.admin.bit.jeap.governance.archrepo.persistence;

import ch.admin.bit.jeap.governance.archrepo.domain.ReactionGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaReactionGraphRepository extends JpaRepository<ReactionGraph, Long> {

    Optional<ReactionGraph> findBySystemComponentName(String componentName);

    Optional<ReactionGraph> findBySystemComponentId(long componentId);
}
