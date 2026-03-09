package ch.admin.bit.jeap.governance.prometheus.datadeletion;

import ch.admin.bit.jeap.governance.domain.plugin.deletion.ComponentDeletionListener;
import ch.admin.bit.jeap.governance.prometheus.domain.PromTimeSeriesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listens to component deletion events and deletes all previously imported Prometheus time series related to the
 * deleted component.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PrometheusComponentDeletionListener implements ComponentDeletionListener {

    private final PromTimeSeriesRepository promTimeSeriesRepository;

    @Override
    @Transactional
    public void preComponentDeletion(long systemComponentId) {
        log.debug("Deleting Prometheus data related to system component with ID '{}'.", systemComponentId);
        int deletedCount = promTimeSeriesRepository.deleteBySystemComponentId(systemComponentId);
        log.debug("Deleted {} Prometheus time series related to system component with ID '{}'.", deletedCount, systemComponentId);
    }

}
