package ch.admin.bit.jeap.governance.archrepo.synchronize;

import ch.admin.bit.jeap.governance.archrepo.connector.model.ArchRepoModelDto;
import ch.admin.bit.jeap.governance.archrepo.connector.model.ArchRepoSystemDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

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
                .build();
        ArchRepoSystemDto systemB = ArchRepoSystemDto.builder()
                .name("System B")
                .build();

        ArchRepoModelDto archRepoModel = ArchRepoModelDto.builder()
                .systems(Arrays.asList(systemA, systemB))
                .build();
        archRepoModelSynchronizer.synchronizeWithArchRepo(archRepoModel);

        verify(archRepoModelSystemSynchronizer).synchronizeWithArchRepo(systemA);
        verify(archRepoModelSystemSynchronizer).synchronizeWithArchRepo(systemB);
        verify(archRepoModelSystemSynchronizer).deleteNoMoreExistingSystems(Arrays.asList("System A", "System B").stream().collect(Collectors.toSet()));
        verifyNoMoreInteractions(archRepoModelSystemSynchronizer);
    }
}
