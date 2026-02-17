package ch.admin.bit.jeap.governance.prometheus.datadeletion;

import ch.admin.bit.jeap.governance.domain.SystemComponentRepository;
import ch.admin.bit.jeap.governance.domain.datasource.ComponentDeletionListener;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeriesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Listens to component deletion events and deletes all previously imported Prometheus time series related to the
 * deleted component.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PrometheusComponentDeletionListener implements ComponentDeletionListener {

    private final SystemComponentRepository systemComponentRepository;
    private final PromTimeSeriesRepository promTimeSeriesRepository;

    @Override
    @Transactional
    public void preComponentDeletion(Long systemComponentId) {
        if (systemComponentId == null) {
            log.warn("Received null systemComponentId for preComponentDeletion.");
            return;
        }

        Optional<String> systemComponentName = systemComponentRepository.findSystemComponentNameById(systemComponentId);
        if (systemComponentName.isEmpty()) {
            log.warn("System component with ID {} not found. Skipping deletion of related Prometheus data.", systemComponentId);
            return;
        }

        log.debug("Deleting Prometheus data related to system component with ID '{}' and name '{}'.", systemComponentId, systemComponentName);
        promTimeSeriesRepository.deleteBy(systemComponentName.get());
        log.debug("Deleted Prometheus data related to system component with ID '{}' and name '{}'.", systemComponentId, systemComponentName);
    }

}
