package ch.admin.bit.jeap.governance.archrepo.domain;

import java.util.List;

public interface RestApiRelationWithoutPactRepository {

    List<RestApiRelationWithoutPact> findAllByProviderSystemComponentId(Long id);

    List<RestApiRelationWithoutPact> findAllByConsumerSystemComponentId(Long id);

    RestApiRelationWithoutPact add(RestApiRelationWithoutPact restApiRelationWithoutPact);

    void delete(RestApiRelationWithoutPact restApiRelationWithoutPact);

    void deleteAllByProviderSystemComponentId(Long id);
}
