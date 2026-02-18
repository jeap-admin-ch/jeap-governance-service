package ch.admin.bit.jeap.governance.archrepo.persistence;

import ch.admin.bit.jeap.governance.archrepo.domain.DatabaseSchemaVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaDatabaseSchemaVersionRepository extends JpaRepository<DatabaseSchemaVersion, Long> {

    Optional<DatabaseSchemaVersion> findBySystemComponentId(long id);

    @Modifying
    @Query("DELETE FROM DatabaseSchemaVersion a WHERE a.systemComponent.system.id = :systemId")
    void deleteAllBySystemId(long systemId);
}
