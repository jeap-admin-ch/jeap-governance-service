package ch.admin.bit.jeap.governance.archrepo.synchronize;

import ch.admin.bit.jeap.governance.archrepo.connector.model.ReactionGraphDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReactionGraphSynchronizer {

    private final ReactionGraphsOneByOneSynchronizer oneByOneSynchronizer;

    public void synchronizeModelWithArchRepo(List<ReactionGraphDto> reactionGraphDtos) {
        boolean hasException = false;
        for (ReactionGraphDto reactionGraphDto : reactionGraphDtos) {
            try {
                oneByOneSynchronizer.synchronizeReactionGraphsLastModifiedAtWithArchRepo(reactionGraphDto);
            } catch (Exception e) {
                // Log and continue with next system to avoid blocking the whole synchronization in case of errors
                log.error("Error synchronizing ReactionGraphDto for system component {}: {}. Proceeding import", reactionGraphDto.getComponent(), e.getMessage(), e);
                hasException = true;
            }
        }
        if (hasException) {
            throw new ArchRepoSynchronizeException("Errors occurred during ReactionGraphDto synchronization. Check logs for details.");
        }
    }
}
