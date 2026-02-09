package ch.admin.bit.jeap.governance.archrepo.persistence;

import ch.admin.bit.jeap.governance.archrepo.domain.DatabaseSchemaVersion;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface JpaDatabaseSchemaVersionRepository extends CrudRepository<DatabaseSchemaVersion, Long> {

    Optional<DatabaseSchemaVersion> findBySystemComponentId(Long id);

    @Modifying
    @Query("DELETE FROM DatabaseSchemaVersion a WHERE a.systemComponent.system.id = :systemId")
    void deleteAllBySystemId(Long systemId);
}
