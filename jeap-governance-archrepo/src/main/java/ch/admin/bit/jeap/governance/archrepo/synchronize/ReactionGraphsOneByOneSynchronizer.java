package ch.admin.bit.jeap.governance.archrepo.synchronize;

import ch.admin.bit.jeap.governance.archrepo.connector.model.ReactionGraphDto;
import ch.admin.bit.jeap.governance.archrepo.domain.ReactionGraph;
import ch.admin.bit.jeap.governance.archrepo.domain.ReactionGraphRepository;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.SystemComponentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReactionGraphsOneByOneSynchronizer {

    private final SystemComponentRepository systemComponentRepository;
    private final ReactionGraphRepository reactionGraphRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void synchronize(ReactionGraphDto reactionGraphDto) {
        Optional<SystemComponent> systemComponentOptional = systemComponentRepository.findByName(reactionGraphDto.getComponent());
        if (systemComponentOptional.isEmpty()) {
            // We do throw an exception here because the system component must exist
            throw new ArchRepoSynchronizeException("System component not found: " + reactionGraphDto.getComponent());
        }
        synchronize(systemComponentOptional.get(), reactionGraphDto);
    }

    private void synchronize(SystemComponent systemComponent, ReactionGraphDto reactionGraphDto) {
        Optional<ReactionGraph> existingReactionGraphOptional = reactionGraphRepository.findByComponentName(reactionGraphDto.getComponent());

        existingReactionGraphOptional.ifPresent(reactionGraphRepository::delete);
        log.info("Creating new ReactionGraph for system component {} with lastModifiedAt: {}", systemComponent.getName(), reactionGraphDto.getLastModifiedAt());
        ReactionGraph newVersion = ReactionGraph.builder()
                .systemComponent(systemComponent)
                .lastModifiedAt(reactionGraphDto.getLastModifiedAt())
                .build();
        reactionGraphRepository.add(newVersion);
    }
}
