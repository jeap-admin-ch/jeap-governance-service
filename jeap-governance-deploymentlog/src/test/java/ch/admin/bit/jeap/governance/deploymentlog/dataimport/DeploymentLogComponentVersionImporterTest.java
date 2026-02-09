package ch.admin.bit.jeap.governance.deploymentlog.dataimport;

import ch.admin.bit.jeap.governance.deploymentlog.connector.DeploymentLogConnector;
import ch.admin.bit.jeap.governance.deploymentlog.connector.model.DeploymentLogComponentVersionDto;
import ch.admin.bit.jeap.governance.deploymentlog.synchronize.DeploymentLogComponentVersionSynchronizer;
import ch.admin.bit.jeap.governance.domain.GovernanceProperties;
import ch.admin.bit.jeap.governance.domain.GovernanceServiceEnvironment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeploymentLogComponentVersionImporterTest {

    @Mock
    private GovernanceProperties properties;

    @Mock
    private DeploymentLogConnector connector;
    @Mock
    private DeploymentLogComponentVersionSynchronizer synchronizer;

    @InjectMocks
    private DeploymentLogComponentVersionImporter importer;


    @Test
    void importData() {
        when(properties.getEnvironment()).thenReturn(GovernanceServiceEnvironment.PROD);

        Set<DeploymentLogComponentVersionDto> dtos = Set.of(mock(DeploymentLogComponentVersionDto.class));
        when(connector.getAllComponentVersions(GovernanceServiceEnvironment.PROD)).thenReturn(dtos);

        importer.importData();

        verify(synchronizer).synchronizeModelWithDeploymentLog(dtos);
        verifyNoMoreInteractions(synchronizer);

        verify(connector).getAllComponentVersions(GovernanceServiceEnvironment.PROD);
        verifyNoMoreInteractions(connector);
    }
}
