package ch.admin.bit.jeap.governance.archrepo.domain;

import java.util.List;

public interface RestApiRelationWithoutPactRepository {

    List<RestApiRelationWithoutPact> findAllByProviderSystemComponentId(long id);

    List<RestApiRelationWithoutPact> findAllByConsumerSystemComponentId(long id);

    RestApiRelationWithoutPact add(RestApiRelationWithoutPact restApiRelationWithoutPact);

    void delete(RestApiRelationWithoutPact restApiRelationWithoutPact);

    void deleteAllByProviderSystemComponentId(long id);
}
