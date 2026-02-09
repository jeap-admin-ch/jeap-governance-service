package ch.admin.bit.jeap.governance.persistence;

import ch.admin.bit.jeap.governance.domain.System;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
interface JpaSystemRepository extends CrudRepository<System, Long> {

    @EntityGraph(attributePaths = {"systemComponents"})
    Optional<System> findByName(String systemName);

    @Override
    @EntityGraph(attributePaths = {"systemComponents"})
    List<System> findAll();
}
