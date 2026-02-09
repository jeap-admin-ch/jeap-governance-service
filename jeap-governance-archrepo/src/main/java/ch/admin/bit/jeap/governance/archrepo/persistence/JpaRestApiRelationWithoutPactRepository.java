package ch.admin.bit.jeap.governance.archrepo.persistence;

import ch.admin.bit.jeap.governance.archrepo.domain.RestApiRelationWithoutPact;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface JpaRestApiRelationWithoutPactRepository extends CrudRepository<RestApiRelationWithoutPact, Long> {

    List<RestApiRelationWithoutPact> findByProviderSystemComponentId(Long id);

    List<RestApiRelationWithoutPact> findByConsumerSystemComponentId(Long id);

    @Modifying
    @Query("DELETE FROM RestApiRelationWithoutPact a WHERE a.providerSystemComponent.id = :id")
    void deleteAllByProviderSystemComponentId(Long id);
}
