package ch.admin.bit.jeap.governance.domain;

import java.util.List;
import java.util.Optional;

/**
 * Interface to be implemented by a persistence provider to access @{@link java.lang.System}s
 */
public interface SystemRepository {
    List<System> findAll();

    Optional<System> findByName(String name);

    System add(System system);

    void delete(System system);

    void update(System system);
}
