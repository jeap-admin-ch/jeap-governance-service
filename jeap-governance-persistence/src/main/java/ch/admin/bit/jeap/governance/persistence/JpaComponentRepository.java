package ch.admin.bit.jeap.governance.persistence;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.SystemComponentReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
interface JpaComponentRepository extends JpaRepository<SystemComponent, Long> {

    Optional<SystemComponent> findByName(String componentName);

    @Query("SELECT sc.id AS id, sc.name AS name FROM SystemComponent sc")
    List<SystemComponentReference> findAllSystemComponentReferences();

}
