package ch.admin.bit.jeap.governance.domain;

import java.util.List;
import java.util.Optional;

/**
 * Interface to be implemented by a persistence provider to access @{@link java.lang.System}s
 */
public interface SystemRepository {
    List<System> findAll();

    List<Long> findAllIds();

    Optional<System> findByName(String name);

    Optional<System> findById(Long id);

    System add(System system);

    void delete(System system);

    void update(System system);

    List<SystemReference> findAllSystemReferences();
}
