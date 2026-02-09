package ch.admin.bit.jeap.governance.domain;

import java.util.Optional;

/**
 * Interface to be implemented by a persistence provider to access @{@link SystemComponent}s
 */
public interface SystemComponentRepository {

    Optional<SystemComponent> findByName(String componentName);

    void deleteById(Long systemComponentId);
}
