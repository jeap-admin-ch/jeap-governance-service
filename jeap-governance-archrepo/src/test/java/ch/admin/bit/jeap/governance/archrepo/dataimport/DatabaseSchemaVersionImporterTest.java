package ch.admin.bit.jeap.governance.archrepo.dataimport;

import ch.admin.bit.jeap.governance.archrepo.connector.ArchRepoConnector;
import ch.admin.bit.jeap.governance.archrepo.connector.model.DatabaseSchemaVersionDto;
import ch.admin.bit.jeap.governance.archrepo.synchronize.DatabaseSchemaVersionSynchronizer;
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
class DatabaseSchemaVersionImporterTest {

    @Mock
    private ArchRepoConnector archRepoConnector;
    @Mock
    private DatabaseSchemaVersionSynchronizer databaseSchemaVersionSynchronizer;

    @InjectMocks
    private DatabaseSchemaVersionImporter databaseSchemaVersionImporter;


    @Test
    void importSystemsFromArchRepo() {
        List<DatabaseSchemaVersionDto> databaseSchemaVersionDtos = List.of(mock(DatabaseSchemaVersionDto.class));
        when(archRepoConnector.getDatabaseSchemaVersions()).thenReturn(databaseSchemaVersionDtos);

        databaseSchemaVersionImporter.importData();

        verify(databaseSchemaVersionSynchronizer).synchronizeModelWithArchRepo(databaseSchemaVersionDtos);
        verifyNoMoreInteractions(databaseSchemaVersionSynchronizer);

        verify(archRepoConnector).getDatabaseSchemaVersions();
        verifyNoMoreInteractions(archRepoConnector);
    }
}
