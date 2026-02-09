package ch.admin.bit.jeap.governance.deploymentlog.synchronize;

import ch.admin.bit.jeap.governance.deploymentlog.connector.model.DeploymentLogComponentVersionDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static ch.admin.bit.jeap.governance.deploymentlog.TestUtility.COMPONENT_NAME_A1;
import static ch.admin.bit.jeap.governance.deploymentlog.TestUtility.COMPONENT_NAME_A2;
import static ch.admin.bit.jeap.governance.deploymentlog.TestUtility.COMPONENT_NAME_B1;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class DeploymentLogComponentVersionSynchronizerTest {

    private static final String VERSION_1_0_0 = "1.0.0";

    @Mock
    private DeploymentLogComponentVersionOneByOneSynchronizer oneByOneSynchronizer;

    @InjectMocks
    private DeploymentLogComponentVersionSynchronizer synchronizer;

    @Test
    void synchronizeModelWithArchRepo_TwoCalls() {
        DeploymentLogComponentVersionDto dto1 = new DeploymentLogComponentVersionDto(COMPONENT_NAME_A1, VERSION_1_0_0);
        DeploymentLogComponentVersionDto dto2 = new DeploymentLogComponentVersionDto(COMPONENT_NAME_A2, VERSION_1_0_0);
        Set<DeploymentLogComponentVersionDto> dtos = Set.of(dto1, dto2);

        synchronizer.synchronizeModelWithDeploymentLog(dtos);

        verify(oneByOneSynchronizer).synchronize(dto1);
        verify(oneByOneSynchronizer).synchronize(dto2);
        verifyNoMoreInteractions(oneByOneSynchronizer);
    }

    @Test
    void synchronizeModelWithArchRepo_severalCalls_ExceptionInFirst() {
        DeploymentLogComponentVersionDto dto1 = new DeploymentLogComponentVersionDto(COMPONENT_NAME_A1, VERSION_1_0_0);
        DeploymentLogComponentVersionDto dto2 = new DeploymentLogComponentVersionDto(COMPONENT_NAME_A2, VERSION_1_0_0);
        DeploymentLogComponentVersionDto dto3 = new DeploymentLogComponentVersionDto(COMPONENT_NAME_B1, VERSION_1_0_0);
        Set<DeploymentLogComponentVersionDto> dtos = Set.of(dto1, dto2, dto3);

        doThrow(new RuntimeException("Something happened")).when(oneByOneSynchronizer).synchronize(dto1);

        assertThatThrownBy(() -> synchronizer.synchronizeModelWithDeploymentLog(dtos)).isInstanceOf(DeploymentLogSynchronizeException.class);

        verify(oneByOneSynchronizer).synchronize(dto1);
        verify(oneByOneSynchronizer).synchronize(dto2);
        verify(oneByOneSynchronizer).synchronize(dto3);
        verifyNoMoreInteractions(oneByOneSynchronizer);
    }
}
