package ch.admin.bit.jeap.governance.archrepo.persistence;

import ch.admin.bit.jeap.governance.archrepo.domain.RestApiRelationWithoutPact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaRestApiRelationWithoutPactRepository extends JpaRepository<RestApiRelationWithoutPact, Long> {

    List<RestApiRelationWithoutPact> findByProviderSystemComponentId(long id);

    List<RestApiRelationWithoutPact> findByConsumerSystemComponentId(long id);

    @Modifying
    @Query("DELETE FROM RestApiRelationWithoutPact")
    void deleteAll();
}
