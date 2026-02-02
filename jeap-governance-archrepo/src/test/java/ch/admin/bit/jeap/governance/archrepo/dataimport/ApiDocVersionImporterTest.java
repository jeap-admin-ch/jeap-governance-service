package ch.admin.bit.jeap.governance.archrepo.dataimport;

import ch.admin.bit.jeap.governance.archrepo.connector.ArchRepoConnector;
import ch.admin.bit.jeap.governance.archrepo.connector.model.ApiDocVersionDto;
import ch.admin.bit.jeap.governance.archrepo.synchronize.ApiDocVersionSynchronizer;
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
class ApiDocVersionImporterTest {

    @Mock
    private ArchRepoConnector archRepoConnector;
    @Mock
    private ApiDocVersionSynchronizer apiDocVersionSynchronizer;

    @InjectMocks
    private ApiDocVersionImporter apiDocVersionImporter;


    @Test
    void importSystemsFromArchRepo() {
        List<ApiDocVersionDto> apiDocVersionDtos = List.of(mock(ApiDocVersionDto.class));
        when(archRepoConnector.getApiDocVersions()).thenReturn(apiDocVersionDtos);

        apiDocVersionImporter.importData();

        verify(apiDocVersionSynchronizer).synchronizeModelWithArchRepo(apiDocVersionDtos);
        verifyNoMoreInteractions(apiDocVersionSynchronizer);

        verify(archRepoConnector).getApiDocVersions();
        verifyNoMoreInteractions(archRepoConnector);
    }
}
