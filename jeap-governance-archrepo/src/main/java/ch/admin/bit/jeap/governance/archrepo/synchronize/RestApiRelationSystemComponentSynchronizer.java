package ch.admin.bit.jeap.governance.archrepo.synchronize;

import ch.admin.bit.jeap.governance.archrepo.connector.model.RestApiRelationWithoutPactDto;
import ch.admin.bit.jeap.governance.archrepo.domain.RestApiRelationWithoutPact;
import ch.admin.bit.jeap.governance.archrepo.domain.RestApiRelationWithoutPactRepository;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.SystemComponentRepository;
import ch.admin.bit.jeap.governance.plugin.api.model.ComponentTechnicalIdentifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class RestApiRelationSystemComponentSynchronizer {

    private final SystemComponentRepository systemComponentRepository;
    private final RestApiRelationWithoutPactRepository restApiRelationWithoutPactRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void synchronizeRestApiRelationWithArchRepo(ComponentTechnicalIdentifier providerKey, List<RestApiRelationWithoutPactDto> restApiRelationDtos) {
        Optional<SystemComponent> providerSystemComponentOptional = systemComponentRepository.findByName(providerKey.componentName());
        if (providerSystemComponentOptional.isEmpty()) {
            // We do throw an exception here because the provider must exist for relations to be valid
            throw new ArchRepoSynchronizeException("Provider system component not found: " + providerKey);
        }
        SystemComponent providerSystemComponent = providerSystemComponentOptional.get();
        validateInput(providerKey, providerSystemComponent);

        restApiRelationWithoutPactRepository.deleteAllByProviderSystemComponentId(providerSystemComponent.getId());
        addAllToSystemComponent(providerSystemComponent, restApiRelationDtos);
    }

    private void addAllToSystemComponent(SystemComponent providerSystemComponent, List<RestApiRelationWithoutPactDto> restApiRelationDtos) {
        log.info("Synchronizing REST API relations for provider: {}", providerSystemComponent.getName());
        for (RestApiRelationWithoutPactDto restApiRelationDto : restApiRelationDtos) {
            Optional<SystemComponent> consumerSystemComponentOptional = systemComponentRepository.findByName(restApiRelationDto.getConsumer());
            if (consumerSystemComponentOptional.isEmpty()) {
                // We do not throw an exception here to allow partial imports
                log.error("Consumer system component not found: {}", restApiRelationDto.getConsumer());
                continue;
            }
            addToSystemComponent(providerSystemComponent, consumerSystemComponentOptional.get(), restApiRelationDto);
        }
        log.info("Synchronizing REST API relations for provider {} done", providerSystemComponent.getName());
    }

    private void addToSystemComponent(SystemComponent providerSystemComponent, SystemComponent consumerSystemComponent, RestApiRelationWithoutPactDto restApiRelationDto) {
        log.info("Creating new REST API relation: {} -> {} {} {}", providerSystemComponent.getName(), consumerSystemComponent.getName(), restApiRelationDto.getMethod(), restApiRelationDto.getPath());
        RestApiRelationWithoutPact newRelation = RestApiRelationWithoutPact.builder()
                .providerSystemComponent(providerSystemComponent)
                .consumerSystemComponent(consumerSystemComponent)
                .method(restApiRelationDto.getMethod())
                .path(restApiRelationDto.getPath())
                .build();
        restApiRelationWithoutPactRepository.add(newRelation);
    }

    private void validateInput(ComponentTechnicalIdentifier providerKey, SystemComponent providerSystemComponent) {
        if (!providerSystemComponent.getSystem().getName().equals(providerKey.systemName())) {
            throw new ArchRepoSynchronizeException("Provider system name mismatch: expected " + providerKey.systemName() + " but found " + providerSystemComponent.getSystem().getName());
        }
        if (!providerSystemComponent.getName().equals(providerKey.componentName())) {
            throw new ArchRepoSynchronizeException("Provider system component name mismatch: expected " + providerKey.componentName() + " but found " + providerSystemComponent.getName());
        }
    }

}
