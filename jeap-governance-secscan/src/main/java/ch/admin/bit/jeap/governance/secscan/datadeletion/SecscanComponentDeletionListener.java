package ch.admin.bit.jeap.governance.secscan.datadeletion;

import ch.admin.bit.jeap.governance.domain.plugin.datasource.ComponentDeletionListener;
import ch.admin.bit.jeap.governance.secscan.domain.SecscanFlaggedEndpointRepository;
import ch.admin.bit.jeap.governance.secscan.domain.SecscanStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listens to component deletion events and deletes all previously imported REST API security scan data related to
 * the deleted component.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class SecscanComponentDeletionListener implements ComponentDeletionListener {

    private final SecscanStateRepository secscanStateRepository;
    private final SecscanFlaggedEndpointRepository secscanFlaggedEndpointRepository;

    @Override
    @Transactional
    public void preComponentDeletion(long systemComponentId) {
        log.debug("Deleting REST API security scan data related to system component with ID '{}'.", systemComponentId);
        secscanFlaggedEndpointRepository.deleteBySystemComponentId(systemComponentId);
        secscanStateRepository.deleteBySystemComponentId(systemComponentId);
        log.debug("Deleted REST API security scan data related to system component with ID '{}'.", systemComponentId);
    }

}
