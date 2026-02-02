package ch.admin.bit.jeap.governance.domain;

import ch.admin.bit.jeap.governance.plugin.api.datasource.ComponentDeletionListener;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ComponentDeletionListenerDelegateTest {

    @Test
    void notifyComponentDeletion_notifyAllListeners() {
        ComponentDeletionListener listener1 = mock(ComponentDeletionListener.class);
        ComponentDeletionListener listener2 = mock(ComponentDeletionListener.class);
        List<ComponentDeletionListener> listeners = List.of(listener1, listener2);

        ComponentDeletionListenerDelegate delegate = new ComponentDeletionListenerDelegate(listeners);

        UUID systemComponentId = UUID.randomUUID();

        delegate.notifyPreComponentDeletion(systemComponentId);

        verify(listener1).preComponentDeletion(systemComponentId);
        verify(listener2).preComponentDeletion(systemComponentId);
    }

    @Test
    void notifyComponentDeletion_notifyAllListenersFirstFails() {
        ComponentDeletionListener listener1 = mock(ComponentDeletionListener.class);
        doThrow(new RuntimeException("Hoppla")).when(listener1).preComponentDeletion(any());
        ComponentDeletionListener listener2 = mock(ComponentDeletionListener.class);
        List<ComponentDeletionListener> listeners = List.of(listener1, listener2);

        ComponentDeletionListenerDelegate delegate = new ComponentDeletionListenerDelegate(listeners);

        UUID systemComponentId = UUID.randomUUID();

        delegate.notifyPreComponentDeletion(systemComponentId);

        verify(listener1).preComponentDeletion(systemComponentId);
        verify(listener2).preComponentDeletion(systemComponentId);
    }

}
