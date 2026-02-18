package ch.admin.bit.jeap.governance.persistence;

import ch.admin.bit.jeap.governance.domain.System;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
interface JpaSystemRepository extends JpaRepository<System, Long> {

    @EntityGraph(attributePaths = {"systemComponents"})
    Optional<System> findByName(String systemName);

    @Override
    @EntityGraph(attributePaths = {"systemComponents"})
    List<System> findAll();

    @Override
    Optional<System> findById(Long id);

    @Query("SELECT s.id FROM System s")
    List<Long> findAllIds();
}
