package ch.admin.bit.jeap.governance.archrepo.persistence;

import ch.admin.bit.jeap.governance.archrepo.domain.ApiDocVersion;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaApiDocVersionRepository extends CrudRepository<ApiDocVersion, UUID> {

    Optional<ApiDocVersion> findBySystemComponentId(UUID id);

    @Modifying
    @Query("DELETE FROM ApiDocVersion a WHERE a.systemComponent.system.id = :systemId")
    void deleteAllBySystemId(UUID systemId);
}
