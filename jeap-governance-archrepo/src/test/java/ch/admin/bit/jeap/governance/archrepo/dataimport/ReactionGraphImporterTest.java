package ch.admin.bit.jeap.governance.archrepo.dataimport;

import ch.admin.bit.jeap.governance.archrepo.connector.ArchRepoConnector;
import ch.admin.bit.jeap.governance.archrepo.connector.model.ReactionGraphDto;
import ch.admin.bit.jeap.governance.archrepo.synchronize.ReactionGraphSynchronizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactionGraphImporterTest {

    @Mock
    private ArchRepoConnector archRepoConnector;
    @Mock
    private ReactionGraphSynchronizer reactionGraphSynchronizer;

    @InjectMocks
    private ReactionGraphImporter reactionGraphImporter;


    @Test
    void importSystemsFromArchRepo() {
        List<ReactionGraphDto> reactionGraphsLastModifiedDtos = List.of(mock(ReactionGraphDto.class));
        when(archRepoConnector.getReactionGraphDtos()).thenReturn(reactionGraphsLastModifiedDtos);

        reactionGraphImporter.importData();

        verify(reactionGraphSynchronizer).synchronizeWithArchRepo(reactionGraphsLastModifiedDtos);
        verifyNoMoreInteractions(reactionGraphSynchronizer);

        verify(archRepoConnector).getReactionGraphDtos();
        verifyNoMoreInteractions(archRepoConnector);
    }
}
