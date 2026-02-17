package ch.admin.bit.jeap.governance.rules;

import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemRepository;
import ch.admin.bit.jeap.governance.domain.rule.State;
import ch.admin.bit.jeap.governance.domain.score.ScoringService;
import net.javacrumbs.shedlock.core.LockAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ScoringSchedulerTest {

    private final ScoringService scoringService = mock(ScoringService.class);
    private final SystemRepository systemRepository = mock(SystemRepository.class);
    private final ScoringScheduler scheduler = new ScoringScheduler(scoringService, systemRepository);

    @BeforeEach
    void setUp() {
        LockAssert.TestHelper.makeAllAssertsPass(true);
    }

    @Test
    void updateScores_callsScoringServiceForEachSystem() {
        System system1 = system("system-1");
        System system2 = system("system-2");
        when(systemRepository.findAll()).thenReturn(List.of(system1, system2));

        scheduler.updateScores();

        var captor = ArgumentCaptor.forClass(System.class);
        verify(scoringService, times(2)).updateSystemScore(captor.capture());
        assertThat(captor.getAllValues()).extracting(System::getName)
                .containsExactly("system-1", "system-2");
    }

    @Test
    void updateScores_oneSystemFails_continuesWithOthers() {
        System system1 = system("system-1");
        System system2 = system("system-2");
        System system3 = system("system-3");
        when(systemRepository.findAll()).thenReturn(List.of(system1, system2, system3));
        doThrow(new RuntimeException("DB error")).when(scoringService).updateSystemScore(system2);

        scheduler.updateScores();

        var captor = ArgumentCaptor.forClass(System.class);
        verify(scoringService, times(3)).updateSystemScore(captor.capture());
        assertThat(captor.getAllValues()).extracting(System::getName)
                .containsExactly("system-1", "system-2", "system-3");
    }

    private System system(String name) {
        return System.builder()
                .name(name)
                .aliases(Set.of())
                .systemComponents(List.of())
                .state(State.OK)
                .build();
    }
}
