package ch.admin.bit.jeap.governance.domain;

import ch.admin.bit.jeap.governance.domain.plugin.deletion.SystemDeletionListener;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SystemService {

    private final SystemRepository systemRepository;
    private final List<SystemDeletionListener> listeners;

    @PostConstruct
    public void init() {
        log.info("Initialized SystemService with these SystemDeletionListener: {}", listeners);
    }

    public void deleteSystem(System system) {
        listeners.forEach(listener -> listener.preSystemDeletion(system.getId()));
        systemRepository.delete(system);
    }

    public Optional<System> findByName(String name) {
        return systemRepository.findByName(name);
    }

    public void update(System updatedSystem) {
        systemRepository.update(updatedSystem);
    }

    public void add(System newSystem) {
        systemRepository.add(newSystem);
    }

    public List<System> findAll() {
        return systemRepository.findAll();
    }
}
