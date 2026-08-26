package ch.admin.bit.jeap.governance.archrepo.synchronize;

import ch.admin.bit.jeap.governance.archrepo.connector.model.ArchRepoModelDto;
import ch.admin.bit.jeap.governance.archrepo.connector.model.ArchRepoSystemComponentDto;
import ch.admin.bit.jeap.governance.archrepo.connector.model.ArchRepoSystemDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArchRepoModelSynchronizerTest {

    @Mock
    private ArchRepoModelSystemSynchronizer archRepoModelSystemSynchronizer;

    @InjectMocks
    private ArchRepoModelSynchronizer archRepoModelSynchronizer;

    @Test
    void synchronizeWithArchRepo() {
        ArchRepoSystemDto systemA = ArchRepoSystemDto.builder()
                .name("System A")
                .systemComponents(List.of(mock(ArchRepoSystemComponentDto.class)))
                .build();
        ArchRepoSystemDto systemB = ArchRepoSystemDto.builder()
                .name("System B")
                .systemComponents(List.of(mock(ArchRepoSystemComponentDto.class)))
                .build();
        ArchRepoSystemDto systemC = ArchRepoSystemDto.builder()
                .name("System C")
                .build();
        ArchRepoSystemDto systemD = ArchRepoSystemDto.builder()
                .name("System D")
                .systemComponents(List.of())
                .build();
        ArchRepoModelDto archRepoModel = ArchRepoModelDto.builder()
                .systems(Arrays.asList(systemA, systemB, systemC, systemD))
                .build();
        archRepoModelSynchronizer.synchronizeWithArchRepo(archRepoModel);

        verify(archRepoModelSystemSynchronizer).synchronizeWithArchRepo(systemA);
        verify(archRepoModelSystemSynchronizer).synchronizeWithArchRepo(systemB);
        verify(archRepoModelSystemSynchronizer, never()).synchronizeWithArchRepo(systemC);
        verify(archRepoModelSystemSynchronizer, never()).synchronizeWithArchRepo(systemD);
        verify(archRepoModelSystemSynchronizer).deleteNoMoreExistingSystems(Set.of("System A", "System B"));
        verifyNoMoreInteractions(archRepoModelSystemSynchronizer);
    }
}
