package ch.admin.bit.jeap.governance.archrepo.dataimport;

import ch.admin.bit.jeap.governance.archrepo.connector.ArchRepoConnector;
import ch.admin.bit.jeap.governance.archrepo.connector.model.ArchRepoModelDto;
import ch.admin.bit.jeap.governance.archrepo.synchronize.ArchRepoModelSynchronizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArchRepoSystemImporterTest {

    @Mock
    private ArchRepoConnector archRepoConnector;
    @Mock
    private ArchRepoModelSynchronizer archRepoModelSynchronizer;

    @InjectMocks
    private ArchRepoSystemImporter archRepoSystemImporter;


    @Test
    void importSystemsFromArchRepo() {
        ArchRepoModelDto archRepoModel = mock(ArchRepoModelDto.class);
        when(archRepoConnector.getModelFromArchRepo()).thenReturn(archRepoModel);

        archRepoSystemImporter.importData();

        verify(archRepoModelSynchronizer).synchronizeModelWithArchRepo(archRepoModel);
        verifyNoMoreInteractions(archRepoModelSynchronizer);

        verify(archRepoConnector).getModelFromArchRepo();
        verifyNoMoreInteractions(archRepoConnector);
    }
}
