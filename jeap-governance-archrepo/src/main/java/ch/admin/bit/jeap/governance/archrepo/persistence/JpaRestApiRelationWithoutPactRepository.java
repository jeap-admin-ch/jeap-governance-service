package ch.admin.bit.jeap.governance.archrepo.persistence;

import ch.admin.bit.jeap.governance.archrepo.domain.RestApiRelationWithoutPact;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

public interface JpaRestApiRelationWithoutPactRepository extends CrudRepository<RestApiRelationWithoutPact, UUID> {

    List<RestApiRelationWithoutPact> findByProviderSystemComponentId(UUID id);

    List<RestApiRelationWithoutPact> findByConsumerSystemComponentId(UUID id);

    @Modifying
    @Query("DELETE FROM RestApiRelationWithoutPact a WHERE a.providerSystemComponent.id = :id")
    void deleteAllByProviderSystemComponentId(UUID id);
}
