package ch.admin.bit.jeap.governance.archrepo.domain;

import java.util.List;
import java.util.UUID;

public interface RestApiRelationWithoutPactRepository {

    List<RestApiRelationWithoutPact> findAllByProviderSystemComponentId(UUID id);

    List<RestApiRelationWithoutPact> findAllByConsumerSystemComponentId(UUID id);

    RestApiRelationWithoutPact add(RestApiRelationWithoutPact restApiRelationWithoutPact);

    void delete(RestApiRelationWithoutPact restApiRelationWithoutPact);

    void deleteAllByProviderSystemComponentId(UUID id);
}
