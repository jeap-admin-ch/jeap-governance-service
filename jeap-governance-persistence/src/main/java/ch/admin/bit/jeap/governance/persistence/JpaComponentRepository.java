package ch.admin.bit.jeap.governance.persistence;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
interface JpaComponentRepository extends CrudRepository<SystemComponent, UUID> {

    Optional<SystemComponent> findByName(String componentName);
}
