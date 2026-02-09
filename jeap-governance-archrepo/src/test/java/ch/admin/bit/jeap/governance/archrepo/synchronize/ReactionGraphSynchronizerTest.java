package ch.admin.bit.jeap.governance.archrepo.synchronize;

import ch.admin.bit.jeap.governance.archrepo.connector.model.ReactionGraphDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;

import static ch.admin.bit.jeap.governance.archrepo.TestUtility.COMPONENT_NAME_A1;
import static ch.admin.bit.jeap.governance.archrepo.TestUtility.COMPONENT_NAME_A2;
import static ch.admin.bit.jeap.governance.archrepo.TestUtility.COMPONENT_NAME_B1;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class ReactionGraphSynchronizerTest {

    @Mock
    private ReactionGraphsOneByOneSynchronizer oneByOneSynchronizer;

    @InjectMocks
    private ReactionGraphSynchronizer synchronizer;

    @Test
    void synchronizeWithArchRepo_TwoCalls() {
        ReactionGraphDto dto1 = new ReactionGraphDto(COMPONENT_NAME_A1, ZonedDateTime.now());
        ReactionGraphDto dto2 = new ReactionGraphDto(COMPONENT_NAME_A2, ZonedDateTime.now());
        List<ReactionGraphDto> dtos = List.of(dto1, dto2);

        synchronizer.synchronizeWithArchRepo(dtos);

        verify(oneByOneSynchronizer).synchronize(dto1);
        verify(oneByOneSynchronizer).synchronize(dto2);
        verifyNoMoreInteractions(oneByOneSynchronizer);
    }

    @Test
    void synchronizeWithArchRepo_severalCalls_ExceptionInFirst() {
        ReactionGraphDto dto1 = new ReactionGraphDto(COMPONENT_NAME_A1, ZonedDateTime.now());
        ReactionGraphDto dto2 = new ReactionGraphDto(COMPONENT_NAME_A2, ZonedDateTime.now());
        ReactionGraphDto dto3 = new ReactionGraphDto(COMPONENT_NAME_B1, ZonedDateTime.now());
        List<ReactionGraphDto> dtos = List.of(dto1, dto2, dto3);

        doThrow(new RuntimeException("Something happened")).when(oneByOneSynchronizer).synchronize(dto1);

        assertThatThrownBy(() -> synchronizer.synchronizeWithArchRepo(dtos)).isInstanceOf(ArchRepoSynchronizeException.class);

        verify(oneByOneSynchronizer).synchronize(dto1);
        verify(oneByOneSynchronizer).synchronize(dto2);
        verify(oneByOneSynchronizer).synchronize(dto3);
        verifyNoMoreInteractions(oneByOneSynchronizer);
    }
}
