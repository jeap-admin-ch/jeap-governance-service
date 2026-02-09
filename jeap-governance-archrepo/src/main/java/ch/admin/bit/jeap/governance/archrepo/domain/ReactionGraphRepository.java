package ch.admin.bit.jeap.governance.archrepo.domain;

import java.util.Optional;

public interface ReactionGraphRepository {

    Optional<ReactionGraph> findByComponentId(Long componentId);

    Optional<ReactionGraph> findByComponentName(String componentName);

    ReactionGraph update(ReactionGraph reactionGraph);

    ReactionGraph add(ReactionGraph reactionGraph);

    void delete(ReactionGraph reactionGraph);
}
