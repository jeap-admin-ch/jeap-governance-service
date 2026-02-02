package ch.admin.bit.jeap.governance.archrepo.persistence;

import ch.admin.bit.jeap.governance.archrepo.domain.RestApiRelationWithoutPact;
import ch.admin.bit.jeap.governance.archrepo.domain.RestApiRelationWithoutPactRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RestApiRelationWithoutPactRepositoryImpl implements RestApiRelationWithoutPactRepository {

    private final JpaRestApiRelationWithoutPactRepository jpaRepository;

    @Override
    public List<RestApiRelationWithoutPact> findAllByProviderSystemComponentId(UUID id) {
        return jpaRepository.findByProviderSystemComponentId(id);
    }

    @Override
    public List<RestApiRelationWithoutPact> findAllByConsumerSystemComponentId(UUID id) {
        return jpaRepository.findByConsumerSystemComponentId(id);
    }

    @Override
    public RestApiRelationWithoutPact add(RestApiRelationWithoutPact restApiRelationWithoutPact) {
        return jpaRepository.save(restApiRelationWithoutPact);
    }

    @Override
    public void delete(RestApiRelationWithoutPact restApiRelationWithoutPact) {
        jpaRepository.delete(restApiRelationWithoutPact);
    }

    @Override
    public void deleteAllByProviderSystemComponentId(UUID id) {
        jpaRepository.deleteAllByProviderSystemComponentId(id);
    }
}
