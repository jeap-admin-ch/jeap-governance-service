package ch.admin.bit.jeap.governance.archrepo.synchronize;

import ch.admin.bit.jeap.governance.archrepo.connector.model.ReactionGraphDto;
import ch.admin.bit.jeap.governance.archrepo.domain.ReactionGraph;
import ch.admin.bit.jeap.governance.archrepo.domain.ReactionGraphRepository;
import ch.admin.bit.jeap.governance.domain.SystemComponentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static ch.admin.bit.jeap.governance.archrepo.TestUtility.COMPONENT_NAME_A1;
import static ch.admin.bit.jeap.governance.archrepo.TestUtility.SYSTEM_COMPONENT_A1;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactionGraphOneByOneSynchronizerTest {

    @Mock
    private SystemComponentRepository systemComponentRepository;
    @Mock
    private ReactionGraphRepository reactionGraphRepository;

    @InjectMocks
    private ReactionGraphsOneByOneSynchronizer synchronizer;

    @Test
    void synchronize_NewEntry() {
        ReactionGraphDto dto = new ReactionGraphDto(COMPONENT_NAME_A1, ZonedDateTime.now());
        when(systemComponentRepository.findByName(COMPONENT_NAME_A1)).thenReturn(Optional.of(SYSTEM_COMPONENT_A1));

        when(reactionGraphRepository.findByComponentName(COMPONENT_NAME_A1)).thenReturn(Optional.empty());

        synchronizer.synchronize(dto);

        ArgumentCaptor<ReactionGraph> captor = ArgumentCaptor.forClass(ReactionGraph.class);
        verify(reactionGraphRepository).add(captor.capture());

        ReactionGraph addedEntity = captor.getValue();
        assertEquals(SYSTEM_COMPONENT_A1, addedEntity.getSystemComponent());
        assertEquals(addedEntity.getLastModifiedAt(), dto.getLastModifiedAt());
        assertNotNull(addedEntity.getCreatedAt());

        verify(reactionGraphRepository).findByComponentName(COMPONENT_NAME_A1);
        verifyNoMoreInteractions(reactionGraphRepository);
    }

    @Test
    void ssynchronize_ReplaceExistingEntry() {
        ReactionGraphDto dto = new ReactionGraphDto(COMPONENT_NAME_A1, ZonedDateTime.now());
        when(systemComponentRepository.findByName(COMPONENT_NAME_A1)).thenReturn(Optional.of(SYSTEM_COMPONENT_A1));

        ReactionGraph existingEntity = ReactionGraph.builder()
                .systemComponent(SYSTEM_COMPONENT_A1)
                .lastModifiedAt(ZonedDateTime.now().minus(1, ChronoUnit.DAYS))
                .build();

        when(reactionGraphRepository.findByComponentName(COMPONENT_NAME_A1)).thenReturn(Optional.of(existingEntity));

        synchronizer.synchronize(dto);

        ArgumentCaptor<ReactionGraph> captor = ArgumentCaptor.forClass(ReactionGraph.class);
        verify(reactionGraphRepository).add(captor.capture());

        ReactionGraph addedEntity = captor.getValue();
        assertEquals(SYSTEM_COMPONENT_A1, addedEntity.getSystemComponent());
        assertEquals(addedEntity.getLastModifiedAt(), dto.getLastModifiedAt());
        assertNotNull(addedEntity.getCreatedAt());

        verify(reactionGraphRepository).findByComponentName(COMPONENT_NAME_A1);
        verify(reactionGraphRepository).delete(existingEntity);
        verify(reactionGraphRepository).add(addedEntity);
        verifyNoMoreInteractions(reactionGraphRepository);
    }

    @Test
    void ssynchronize_ThrowExceptionWhenSystemNotFound() {
        ReactionGraphDto dto = new ReactionGraphDto(COMPONENT_NAME_A1, ZonedDateTime.now());
        when(systemComponentRepository.findByName(COMPONENT_NAME_A1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> synchronizer.synchronize(dto)).isInstanceOf(ArchRepoSynchronizeException.class);

        verifyNoInteractions(reactionGraphRepository);
    }
}
