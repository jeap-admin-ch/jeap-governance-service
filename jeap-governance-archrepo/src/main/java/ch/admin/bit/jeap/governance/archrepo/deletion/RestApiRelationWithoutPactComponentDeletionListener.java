package ch.admin.bit.jeap.governance.archrepo.deletion;

import ch.admin.bit.jeap.governance.archrepo.domain.RestApiRelationWithoutPact;
import ch.admin.bit.jeap.governance.archrepo.domain.RestApiRelationWithoutPactRepository;
import ch.admin.bit.jeap.governance.domain.plugin.datasource.ComponentDeletionListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name="jeap.governance.archrepo.import.restapirelationwithoutpact.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class RestApiRelationWithoutPactComponentDeletionListener implements ComponentDeletionListener {

    private final RestApiRelationWithoutPactRepository repository;

    @Override
    @Transactional
    public void preComponentDeletion(long systemComponentId) {
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
