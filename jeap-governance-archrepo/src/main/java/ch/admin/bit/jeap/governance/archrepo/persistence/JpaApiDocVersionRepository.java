package ch.admin.bit.jeap.governance.archrepo.persistence;

import ch.admin.bit.jeap.governance.archrepo.domain.ApiDocVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaApiDocVersionRepository extends JpaRepository<ApiDocVersion, Long> {

    Optional<ApiDocVersion> findBySystemComponentId(long id);

    @Modifying
    @Query("DELETE FROM ApiDocVersion a WHERE a.systemComponent.system.id = :systemId")
    void deleteAllBySystemId(long systemId);
}
