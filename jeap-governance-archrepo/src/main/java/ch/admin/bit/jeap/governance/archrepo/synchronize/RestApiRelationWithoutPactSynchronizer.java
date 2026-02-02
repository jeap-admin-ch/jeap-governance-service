package ch.admin.bit.jeap.governance.archrepo.synchronize;

import ch.admin.bit.jeap.governance.archrepo.connector.model.RestApiRelationWithoutPactDto;
import ch.admin.bit.jeap.governance.plugin.api.model.ComponentTechnicalIdentifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class RestApiRelationWithoutPactSynchronizer {

    private final RestApiRelationSystemComponentSynchronizer restApiRelationSystemComponentSynchronizer;

    public void synchronizeModelWithArchRepo(List<RestApiRelationWithoutPactDto> restApiRelationDtos) {
        boolean hasException = false;
        // Group relations by provider system and component to minimize transaction size
        Map<ComponentTechnicalIdentifier, List<RestApiRelationWithoutPactDto>> relationsByProvider = groupForProviders(restApiRelationDtos);
        for (Map.Entry<ComponentTechnicalIdentifier, List<RestApiRelationWithoutPactDto>> entry : relationsByProvider.entrySet()) {
            try {
                restApiRelationSystemComponentSynchronizer.synchronizeRestApiRelationWithArchRepo(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                // Log and continue with next provider to avoid blocking the whole synchronization in case of errors
                log.error("Error synchronizing REST API relations for provider {}: {}. Proceeding import", entry.getKey(), e.getMessage(), e);
                hasException = true;
            }
        }
        if (hasException) {
            throw new ArchRepoSynchronizeException("Errors occurred during REST API relation synchronization. Check logs for details.");
        }
    }

    private static Map<ComponentTechnicalIdentifier, List<RestApiRelationWithoutPactDto>> groupForProviders(List<RestApiRelationWithoutPactDto> restApiRelationDtos) {
        return restApiRelationDtos.stream()
                .collect(Collectors.groupingBy(dto ->
                        new ComponentTechnicalIdentifier(dto.getProviderSystem(), dto.getProvider())
                ));
    }
}
