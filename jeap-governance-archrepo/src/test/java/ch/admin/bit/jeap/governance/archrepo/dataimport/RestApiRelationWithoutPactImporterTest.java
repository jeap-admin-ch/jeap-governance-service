package ch.admin.bit.jeap.governance.archrepo.dataimport;

import ch.admin.bit.jeap.governance.archrepo.connector.ArchRepoConnector;
import ch.admin.bit.jeap.governance.archrepo.connector.model.RestApiRelationWithoutPactDto;
import ch.admin.bit.jeap.governance.archrepo.synchronize.RestApiRelationWithoutPactSynchronizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestApiRelationWithoutPactImporterTest {

    @Mock
    private ArchRepoConnector archRepoConnector;
    @Mock
    private RestApiRelationWithoutPactSynchronizer restApiRelationWithoutPactSynchronizer;

    @InjectMocks
    private RestApiRelationWithoutPactImporter restApiRelationWithoutPactImporter;

    @Test
    void importData() {
        RestApiRelationWithoutPactDto dto = new RestApiRelationWithoutPactDto("ConsumerSystem1", "Consumer1", "ProviderSystem1", "Provider1", "GET", "/api/resource1");
        when(archRepoConnector.getRestRelationWithoutPact()).thenReturn(List.of(dto));

        restApiRelationWithoutPactImporter.importData();

        verify(restApiRelationWithoutPactSynchronizer).synchronizeModelWithArchRepo(List.of(dto));
        verifyNoMoreInteractions(restApiRelationWithoutPactSynchronizer);
    }
}
