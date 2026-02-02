package ch.admin.bit.jeap.governance.domain;

import ch.admin.bit.jeap.governance.plugin.api.datasource.ComponentDeletionListener;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ComponentDeletionListenerDelegate {

    private final List<ComponentDeletionListener> listeners;

    @PostConstruct
    public void init() {
        log.info("Initialized ComponentDeletionListenerDelegate with these listeners: {}", listeners);
    }

    public void notifyPreComponentDeletion(UUID systemComponentId) {
        for (ComponentDeletionListener listener : listeners) {
            try {
                listener.preComponentDeletion(systemComponentId);
            } catch (Exception e) {
                log.error("Error while notifying listener {} about component deletion of component with id {}", listener.getClass().getName(), systemComponentId, e);
            }
        }
    }
}
