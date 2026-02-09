package ch.admin.bit.jeap.governance.deploymentlog.synchronize;

import ch.admin.bit.jeap.governance.deploymentlog.connector.model.DeploymentLogComponentVersionDto;
import ch.admin.bit.jeap.governance.deploymentlog.domain.DeploymentLogComponentVersion;
import ch.admin.bit.jeap.governance.deploymentlog.domain.DeploymentLogComponentVersionRepository;
import ch.admin.bit.jeap.governance.domain.SystemComponentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static ch.admin.bit.jeap.governance.deploymentlog.TestUtility.COMPONENT_NAME_A1;
import static ch.admin.bit.jeap.governance.deploymentlog.TestUtility.SYSTEM_COMPONENT_A1;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeploymentLogComponentVersionOneByOneSynchronizerTest {

    private static final String VERSION_1_0_0 = "1.0.0";
    private static final String VERSION_1_0_1 = "1.0.1";

    @Mock
    private SystemComponentRepository systemComponentRepository;
    @Mock
    private DeploymentLogComponentVersionRepository deploymentLogComponentVersionRepository;

    @InjectMocks
    private DeploymentLogComponentVersionOneByOneSynchronizer synchronizer;

    @Test
    void synchronize_NewEntry() {
        DeploymentLogComponentVersionDto dto = new DeploymentLogComponentVersionDto(COMPONENT_NAME_A1, VERSION_1_0_0);
        when(systemComponentRepository.findByName(COMPONENT_NAME_A1)).thenReturn(Optional.of(SYSTEM_COMPONENT_A1));

        when(deploymentLogComponentVersionRepository.findByComponentName(COMPONENT_NAME_A1)).thenReturn(Optional.empty());

        synchronizer.synchronize(dto);

        ArgumentCaptor<DeploymentLogComponentVersion> captor = ArgumentCaptor.forClass(DeploymentLogComponentVersion.class);
        verify(deploymentLogComponentVersionRepository).add(captor.capture());

        DeploymentLogComponentVersion addedEntity = captor.getValue();
        assertEquals(SYSTEM_COMPONENT_A1, addedEntity.getSystemComponent());
        assertEquals(VERSION_1_0_0, dto.version());
        assertNotNull(addedEntity.getCreatedAt());

        verify(deploymentLogComponentVersionRepository).findByComponentName(COMPONENT_NAME_A1);
        verify(deploymentLogComponentVersionRepository).add(addedEntity);
        verifyNoMoreInteractions(deploymentLogComponentVersionRepository);
    }

    @Test
    void synchronize_ReplaceExistingEntry() {
        DeploymentLogComponentVersionDto dto = new DeploymentLogComponentVersionDto(COMPONENT_NAME_A1, VERSION_1_0_1);
        when(systemComponentRepository.findByName(COMPONENT_NAME_A1)).thenReturn(Optional.of(SYSTEM_COMPONENT_A1));

        DeploymentLogComponentVersion existingEntity = DeploymentLogComponentVersion.builder()
                .systemComponent(SYSTEM_COMPONENT_A1)
                .version(VERSION_1_0_0)
                .build();

        when(deploymentLogComponentVersionRepository.findByComponentName(COMPONENT_NAME_A1)).thenReturn(Optional.of(existingEntity));

        synchronizer.synchronize(dto);

        ArgumentCaptor<DeploymentLogComponentVersion> captor = ArgumentCaptor.forClass(DeploymentLogComponentVersion.class);
        verify(deploymentLogComponentVersionRepository).add(captor.capture());

        DeploymentLogComponentVersion addedEntity = captor.getValue();
        assertEquals(SYSTEM_COMPONENT_A1, addedEntity.getSystemComponent());
        assertEquals(VERSION_1_0_1, dto.version());
        assertNotNull(addedEntity.getCreatedAt());

        verify(deploymentLogComponentVersionRepository).findByComponentName(COMPONENT_NAME_A1);
        verify(deploymentLogComponentVersionRepository).delete(existingEntity);
        verify(deploymentLogComponentVersionRepository).add(addedEntity);
        verifyNoMoreInteractions(deploymentLogComponentVersionRepository);
    }

    @Test
    void synchronizeReactionGraphWithArchRepo_DoNothingWhenSystemNotFound() {
        DeploymentLogComponentVersionDto dto = new DeploymentLogComponentVersionDto(COMPONENT_NAME_A1, VERSION_1_0_0);
        when(systemComponentRepository.findByName(COMPONENT_NAME_A1)).thenReturn(Optional.empty());

        synchronizer.synchronize(dto);

        verifyNoInteractions(deploymentLogComponentVersionRepository);
    }
}
