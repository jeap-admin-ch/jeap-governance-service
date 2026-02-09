package ch.admin.bit.jeap.governance.archrepo.synchronize;

import ch.admin.bit.jeap.governance.archrepo.connector.model.ArchRepoSystemDto;
import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemComponentService;
import ch.admin.bit.jeap.governance.domain.SystemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.LongConsumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArchRepoModelSystemSynchronizerTest {

    @Mock
    private SystemRepository systemRepository;
    @Mock
    private SystemComponentService systemComponentService;
    @Mock
    private ArchRepoModelSystemUpdater archRepoModelSystemUpdater;

    @InjectMocks
    private ArchRepoModelSystemSynchronizer archRepoModelSystemSynchronizer;

    @Test
    void synchronizeSystemWithArchRepo_createNew() {
        String systemName = "System A";
        when(systemRepository.findByName(systemName)).thenReturn(Optional.empty());

        ArchRepoSystemDto archRepoSystem = ArchRepoSystemDto.builder()
                .name(systemName)
                .build();

        System newSystem = mock(System.class);
        when(archRepoModelSystemUpdater.createNewSystem(archRepoSystem)).thenReturn(newSystem);

        archRepoModelSystemSynchronizer.synchronizeWithArchRepo(archRepoSystem);

        verify(systemRepository).findByName(systemName);
        verify(systemRepository).add(newSystem);
        verifyNoMoreInteractions(systemRepository);
        verify(archRepoModelSystemUpdater).createNewSystem(archRepoSystem);
        verifyNoMoreInteractions(archRepoModelSystemUpdater);
    }

    @Test
    void synchronizeSystemWithArchRepo_updateExisting() {
        System existingSystem = mock(System.class);
        String systemName = "System A";
        when(systemRepository.findByName(systemName)).thenReturn(Optional.of(existingSystem));

        ArchRepoSystemDto archRepoSystem = ArchRepoSystemDto.builder()
                .name(systemName)
                .build();
        System updatedSystem = mock(System.class);
        when(archRepoModelSystemUpdater.updateSystem(eq(existingSystem), eq(archRepoSystem), any(LongConsumer.class))).thenReturn(updatedSystem);


        archRepoModelSystemSynchronizer.synchronizeWithArchRepo(archRepoSystem);

        verify(systemRepository).findByName(systemName);
        verify(systemRepository).update(updatedSystem);
        verifyNoMoreInteractions(systemRepository);
        verify(archRepoModelSystemUpdater).updateSystem(eq(existingSystem), eq(archRepoSystem), any(LongConsumer.class));
        verifyNoMoreInteractions(archRepoModelSystemUpdater);
    }

    @Test
    void deleteNoMoreExistingSystems() {
        String systemNameA = "System A";
        String systemNameB = "System B";
        String systemNameC = "System C";
        String systemNameD = "System D";
        System existingSystemA = createSystem(systemNameA);
        System existingSystemB = createSystem(systemNameB);
        System existingSystemC = createSystem(systemNameC);
        System existingSystemD = createSystem(systemNameD);

        when(systemRepository.findAll()).thenReturn(List.of(existingSystemA, existingSystemB, existingSystemC, existingSystemD));

        archRepoModelSystemSynchronizer.deleteNoMoreExistingSystems(Set.of(
                systemNameA,
                systemNameD
        ));

        verify(systemRepository).findAll();
        verify(systemRepository).delete(existingSystemB);
        verify(systemRepository).delete(existingSystemC);
        verifyNoMoreInteractions(systemRepository);
    }

    @Test
    void deleteSystemComponent() {
        Long systemComponentId = 42L;
        archRepoModelSystemSynchronizer.deleteSystemComponent(systemComponentId);

        verify(systemComponentService).deleteById(systemComponentId);
        verifyNoMoreInteractions(systemComponentService);
    }

    private System createSystem(String systemName) {
        System aSystem = mock(System.class);
        when(aSystem.getName()).thenReturn(systemName);
        return aSystem;
    }
}
