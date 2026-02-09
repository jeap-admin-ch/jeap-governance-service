package ch.admin.bit.jeap.governance.archrepo.persistence;

import ch.admin.bit.jeap.governance.archrepo.domain.ApiDocVersion;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface JpaApiDocVersionRepository extends CrudRepository<ApiDocVersion, Long> {

    Optional<ApiDocVersion> findBySystemComponentId(Long id);

    @Modifying
    @Query("DELETE FROM ApiDocVersion a WHERE a.systemComponent.system.id = :systemId")
    void deleteAllBySystemId(Long systemId);
}
