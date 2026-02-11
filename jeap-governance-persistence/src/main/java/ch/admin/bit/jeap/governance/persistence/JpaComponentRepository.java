package ch.admin.bit.jeap.governance.persistence;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
interface JpaComponentRepository extends CrudRepository<SystemComponent, Long> {

    Optional<SystemComponent> findByName(String componentName);

    @Query("select distinct sc.name from SystemComponent sc")
    Set<String> findAllSystemComponentNames();

    @Query("select sc.name from SystemComponent sc where sc.id = :systemComponentId")
    Optional<String> findSystemComponentNameById(@Param("systemComponentId") long systemComponentId);

}
