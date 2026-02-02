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
import java.util.UUID;

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
    void synchronizeReactionGraphsLastModifiedAtWithArchRepo_NewEntry() {
        ReactionGraphDto dto = new ReactionGraphDto(COMPONENT_NAME_A1, ZonedDateTime.now());
        when(systemComponentRepository.findByName(COMPONENT_NAME_A1)).thenReturn(Optional.of(SYSTEM_COMPONENT_A1));

        when(reactionGraphRepository.findByComponentName(COMPONENT_NAME_A1)).thenReturn(Optional.empty());

        synchronizer.synchronizeReactionGraphsLastModifiedAtWithArchRepo(dto);

        ArgumentCaptor<ReactionGraph> captor = ArgumentCaptor.forClass(ReactionGraph.class);
        verify(reactionGraphRepository).add(captor.capture());

        ReactionGraph addedEntity = captor.getValue();
        assertEquals(SYSTEM_COMPONENT_A1, addedEntity.getSystemComponent());
        assertEquals(addedEntity.getLastModifiedAt(), dto.getLastModifiedAt());
        assertNotNull(addedEntity.getId());
        assertNotNull(addedEntity.getCreatedAt());

        verify(reactionGraphRepository).findByComponentName(COMPONENT_NAME_A1);
        verifyNoMoreInteractions(reactionGraphRepository);
    }

    @Test
    void synchronizeReactionGraphsLastModifiedAtWithArchRepo_UpdateExistingEntry() {
        ReactionGraphDto dto = new ReactionGraphDto(COMPONENT_NAME_A1, ZonedDateTime.now());
        when(systemComponentRepository.findByName(COMPONENT_NAME_A1)).thenReturn(Optional.of(SYSTEM_COMPONENT_A1));

        ReactionGraph existingEntity = ReactionGraph.builder()
                .id(UUID.randomUUID())
                .systemComponent(SYSTEM_COMPONENT_A1)
                .lastModifiedAt(ZonedDateTime.now().minus(1, ChronoUnit.DAYS))
                .build();

        when(reactionGraphRepository.findByComponentName(COMPONENT_NAME_A1)).thenReturn(Optional.of(existingEntity));

        synchronizer.synchronizeReactionGraphsLastModifiedAtWithArchRepo(dto);

        ArgumentCaptor<ReactionGraph> captor = ArgumentCaptor.forClass(ReactionGraph.class);
        verify(reactionGraphRepository).update(captor.capture());

        ReactionGraph updatedEntity = captor.getValue();
        assertEquals(SYSTEM_COMPONENT_A1, updatedEntity.getSystemComponent());
        assertEquals(updatedEntity.getLastModifiedAt(), dto.getLastModifiedAt());
        assertNotNull(updatedEntity.getId());
        assertNotNull(updatedEntity.getCreatedAt());


        verify(reactionGraphRepository).findByComponentName(COMPONENT_NAME_A1);
        verifyNoMoreInteractions(reactionGraphRepository);
    }

    @Test
    void synchronizeReactionGraphsLastModifiedAtWithArchRepo_UnchangedExistingEntry() {
        ReactionGraphDto dto = new ReactionGraphDto(COMPONENT_NAME_A1, ZonedDateTime.now());
        when(systemComponentRepository.findByName(COMPONENT_NAME_A1)).thenReturn(Optional.of(SYSTEM_COMPONENT_A1));

        ReactionGraph existingEntity = ReactionGraph.builder()
                .id(UUID.randomUUID())
                .systemComponent(SYSTEM_COMPONENT_A1)
                .lastModifiedAt(dto.getLastModifiedAt())
                .build();

        when(reactionGraphRepository.findByComponentName(COMPONENT_NAME_A1)).thenReturn(Optional.of(existingEntity));

        synchronizer.synchronizeReactionGraphsLastModifiedAtWithArchRepo(dto);

        verify(reactionGraphRepository).findByComponentName(COMPONENT_NAME_A1);
        verifyNoMoreInteractions(reactionGraphRepository);
    }

    @Test
    void synchronizeReactionGraphsLastModifiedAtWithArchRepo_ThrowExceptionWhenSystemNotFound() {
        ReactionGraphDto dto = new ReactionGraphDto(COMPONENT_NAME_A1, ZonedDateTime.now());
        when(systemComponentRepository.findByName(COMPONENT_NAME_A1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> synchronizer.synchronizeReactionGraphsLastModifiedAtWithArchRepo(dto)).isInstanceOf(ArchRepoSynchronizeException.class);

        verifyNoInteractions(reactionGraphRepository);
    }
}
