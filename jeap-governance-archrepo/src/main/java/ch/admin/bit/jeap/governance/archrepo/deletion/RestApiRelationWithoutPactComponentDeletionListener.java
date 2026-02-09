package ch.admin.bit.jeap.governance.archrepo.deletion;

import ch.admin.bit.jeap.governance.archrepo.domain.RestApiRelationWithoutPact;
import ch.admin.bit.jeap.governance.archrepo.domain.RestApiRelationWithoutPactRepository;
import ch.admin.bit.jeap.governance.plugin.api.datasource.ComponentDeletionListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty("jeap.governance.archrepo.import.restapirelationwithoutpact.enabled")
@Slf4j
public class RestApiRelationWithoutPactComponentDeletionListener implements ComponentDeletionListener {

    private final RestApiRelationWithoutPactRepository repository;

    @Override
    @Transactional
    public void preComponentDeletion(Long systemComponentId) {
        log.debug("Deleting RestApiRelationWithoutPact entities related to system component with ID: {}", systemComponentId);
        for (RestApiRelationWithoutPact restApiRelationWithoutPact : repository.findAllByProviderSystemComponentId(systemComponentId)) {
            repository.delete(restApiRelationWithoutPact);
        }
        for (RestApiRelationWithoutPact restApiRelationWithoutPact : repository.findAllByConsumerSystemComponentId(systemComponentId)) {
            repository.delete(restApiRelationWithoutPact);
        }
        log.debug("Deletion done");
    }
}
