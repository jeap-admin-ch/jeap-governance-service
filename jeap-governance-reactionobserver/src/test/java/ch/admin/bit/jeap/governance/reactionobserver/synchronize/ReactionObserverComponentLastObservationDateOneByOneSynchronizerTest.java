package ch.admin.bit.jeap.governance.reactionobserver.synchronize;

import ch.admin.bit.jeap.governance.domain.SystemComponentRepository;
import ch.admin.bit.jeap.governance.reactionobserver.domain.ReactionObserverComponentLastObservationDate;
import ch.admin.bit.jeap.governance.reactionobserver.domain.ReactionObserverComponentLastObservationDateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static ch.admin.bit.jeap.governance.reactionobserver.TestUtility.createSystem;
import static ch.admin.bit.jeap.governance.reactionobserver.TestUtility.createSystemComponent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactionObserverComponentLastObservationDateOneByOneSynchronizerTest {

    @Mock
    private SystemComponentRepository systemComponentRepository;
    @Mock
    private ReactionObserverComponentLastObservationDateRepository lastObservationDateRepository;

    @InjectMocks
    private ReactionObserverComponentLastObservationDateOneByOneSynchronizer synchronizer;

    @Test
    void synchronize_newEntry() {
        LocalDate lastObservationDate = LocalDate.now();
        when(systemComponentRepository.findByName("component1")).thenReturn(Optional.of(createSystemComponent(createSystem("test"), "component1")));

        when(lastObservationDateRepository.findByComponentName("component1")).thenReturn(Optional.empty());

        synchronizer.synchronize("component1", lastObservationDate);

        ArgumentCaptor<ReactionObserverComponentLastObservationDate> captor = ArgumentCaptor.forClass(ReactionObserverComponentLastObservationDate.class);
        verify(lastObservationDateRepository).add(captor.capture());

        ReactionObserverComponentLastObservationDate addedEntity = captor.getValue();
        assertThat(addedEntity.getSystemComponent().getName()).isEqualTo("component1");
        assertThat(addedEntity.getLastObservationDate()).isEqualTo(lastObservationDate);
        assertThat(addedEntity.getCreatedAt()).isNotNull();

        verify(lastObservationDateRepository).findByComponentName("component1");
        verify(lastObservationDateRepository).add(addedEntity);
        verifyNoMoreInteractions(lastObservationDateRepository);
    }

    @Test
    void synchronize_replaceExistingEntry() {
        LocalDate lastObservationDate = LocalDate.now().minusDays(1);
        LocalDate lastObservationDateNew = LocalDate.now();
        when(systemComponentRepository.findByName("component1")).thenReturn(Optional.of(createSystemComponent(createSystem("test"), "component1")));

        ReactionObserverComponentLastObservationDate existingEntity = ReactionObserverComponentLastObservationDate.builder()
                .lastObservationDate(lastObservationDate)
                .build();

        when(lastObservationDateRepository.findByComponentName("component1")).thenReturn(Optional.of(existingEntity));

        synchronizer.synchronize("component1", lastObservationDateNew);

        ArgumentCaptor<ReactionObserverComponentLastObservationDate> captor = ArgumentCaptor.forClass(ReactionObserverComponentLastObservationDate.class);
        verify(lastObservationDateRepository).add(captor.capture());

        ReactionObserverComponentLastObservationDate addedEntity = captor.getValue();
        assertThat(addedEntity.getSystemComponent().getName()).isEqualTo("component1");
        assertThat(addedEntity.getLastObservationDate()).isEqualTo(lastObservationDateNew);
        assertThat(addedEntity.getCreatedAt()).isNotNull();

        verify(lastObservationDateRepository).findByComponentName("component1");
        verify(lastObservationDateRepository).delete(existingEntity);
        verify(lastObservationDateRepository).add(addedEntity);
        verifyNoMoreInteractions(lastObservationDateRepository);
    }

    @Test
    void synchronize_doNothingWhenSystemNotFound() {
        when(systemComponentRepository.findByName("component1")).thenReturn(Optional.empty());

        synchronizer.synchronize("component1", LocalDate.now());

        verifyNoInteractions(lastObservationDateRepository);
    }
}
