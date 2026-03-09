package ch.admin.bit.jeap.governance.domain;

import ch.admin.bit.jeap.governance.domain.plugin.deletion.SystemDeletionListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemServiceTest {

    private SystemRepository systemRepository;
    private SystemDeletionListener listener1;
    private SystemDeletionListener listener2;
    private List<SystemDeletionListener> listeners;
    private SystemService systemService;

    @BeforeEach
    void setUp() {
        systemRepository = mock(SystemRepository.class);
        listener1 = mock(SystemDeletionListener.class);
        listener2 = mock(SystemDeletionListener.class);
        listeners = List.of(listener1, listener2);
        systemService = new SystemService(systemRepository, listeners);
    }

    @Test
    void deleteSystem() {
        System system = mock(System.class);
        when(system.getId()).thenReturn(42L);

        systemService.deleteSystem(system);

        verify(listener1).preSystemDeletion(42L);
        verify(listener2).preSystemDeletion(42L);
        verify(systemRepository).delete(system);
    }

    @Test
    void findByName() {
        System system = mock(System.class);
        when(systemRepository.findByName("TestSystem")).thenReturn(Optional.of(system));

        Optional<System> result = systemService.findByName("TestSystem");
        assertEquals(Optional.of(system), result);
    }

    @Test
    void update() {
        System system = mock(System.class);

        systemService.update(system);

        verify(systemRepository).update(system);
    }

    @Test
    void add() {
        System system = mock(System.class);

        systemService.add(system);

        verify(systemRepository).add(system);
    }

    @Test
    void findAll() {
        System system = mock(System.class);
        List<System> systems = List.of(system);
        when(systemRepository.findAll()).thenReturn(systems);

        List<System> result = systemService.findAll();

        assertEquals(systems, result);
    }
}
