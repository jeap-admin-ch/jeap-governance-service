package ch.admin.bit.jeap.governance.archrepo.dataimport;

import ch.admin.bit.jeap.governance.archrepo.connector.ArchRepoConnector;
import ch.admin.bit.jeap.governance.archrepo.connector.model.ReactionGraphDto;
import ch.admin.bit.jeap.governance.archrepo.synchronize.ReactionGraphSynchronizer;
import ch.admin.bit.jeap.governance.plugin.api.datasource.DataSourceImporter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

import static ch.admin.bit.jeap.governance.archrepo.dataimport.ImportOrder.REACTION_GRAPHS_LAST_MODIFIED_AT_ORDER;

@Component
@Order(REACTION_GRAPHS_LAST_MODIFIED_AT_ORDER)
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name="jeap.governance.archrepo.import.reactiongraph.enabled", havingValue = "true", matchIfMissing = true)
public class ReactionGraphImporter implements DataSourceImporter {

    private final ArchRepoConnector archRepoConnector;
    private final ReactionGraphSynchronizer reactionGraphSynchronizer;

    @Override
    public void importData() {
        log.info("Start synchronization with arch repo");
        List<ReactionGraphDto> reactionGraphDtos = archRepoConnector.getReactionGraphDtos();
        log.debug("Got model from arch repo: {}", reactionGraphDtos);
        reactionGraphSynchronizer.synchronizeWithArchRepo(reactionGraphDtos);
        log.info("Finished synchronization with arch repo");
    }
}
