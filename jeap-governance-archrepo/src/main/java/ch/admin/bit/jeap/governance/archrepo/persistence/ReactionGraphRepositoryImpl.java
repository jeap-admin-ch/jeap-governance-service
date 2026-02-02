package ch.admin.bit.jeap.governance.archrepo.persistence;

import ch.admin.bit.jeap.governance.archrepo.domain.ReactionGraph;
import ch.admin.bit.jeap.governance.archrepo.domain.ReactionGraphRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ReactionGraphRepositoryImpl implements ReactionGraphRepository {

    private final JpaReactionGraphRepository jpaRepository;

    @Override
    public Optional<ReactionGraph> findByComponentId(UUID componentId) {
        return jpaRepository.findBySystemComponentId(componentId);
    }

    @Override
    public Optional<ReactionGraph> findByComponentName(String componentName) {
        return jpaRepository.findBySystemComponentName(componentName);
    }

    @Override
    public ReactionGraph update(ReactionGraph reactionGraph) {
        return jpaRepository.save(reactionGraph);
    }

    @Override
    public ReactionGraph add(ReactionGraph reactionGraph) {
        return jpaRepository.save(reactionGraph);
    }

    @Override
    public void delete(ReactionGraph reactionGraph) {
        jpaRepository.delete(reactionGraph);
    }
}
