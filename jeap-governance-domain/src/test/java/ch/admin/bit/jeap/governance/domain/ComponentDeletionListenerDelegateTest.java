package ch.admin.bit.jeap.governance.domain;

import ch.admin.bit.jeap.governance.domain.plugin.deletion.ComponentDeletionListener;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

class ComponentDeletionListenerDelegateTest {

    @Test
    void notifyComponentDeletion_notifyAllListeners() {
        ComponentDeletionListener listener1 = mock(ComponentDeletionListener.class);
        ComponentDeletionListener listener2 = mock(ComponentDeletionListener.class);
        List<ComponentDeletionListener> listeners = List.of(listener1, listener2);

        ComponentDeletionListenerDelegate delegate = new ComponentDeletionListenerDelegate(listeners);

        long systemComponentId = 42L;

        delegate.notifyPreComponentDeletion(systemComponentId);

        verify(listener1).preComponentDeletion(systemComponentId);
        verify(listener2).preComponentDeletion(systemComponentId);
    }

    @Test
    void notifyComponentDeletion_notifyAllListenersFirstFails() {
        ComponentDeletionListener listener1 = mock(ComponentDeletionListener.class);
        doThrow(new RuntimeException("Hoppla")).when(listener1).preComponentDeletion(anyLong());
        ComponentDeletionListener listener2 = mock(ComponentDeletionListener.class);
        List<ComponentDeletionListener> listeners = List.of(listener1, listener2);

        ComponentDeletionListenerDelegate delegate = new ComponentDeletionListenerDelegate(listeners);

        long systemComponentId = 42L;

        delegate.notifyPreComponentDeletion(systemComponentId);

        verify(listener1).preComponentDeletion(systemComponentId);
        verify(listener2).preComponentDeletion(systemComponentId);
    }

}
