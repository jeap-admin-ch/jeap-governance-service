package ch.admin.bit.jeap.governance.reactionobserver.dataimport;

import ch.admin.bit.jeap.governance.domain.plugin.datasource.DataSourceImporter;
import ch.admin.bit.jeap.governance.reactionobserver.connector.ReactionObserverConnector;
import ch.admin.bit.jeap.governance.reactionobserver.synchronize.ReactionObserverComponentLastObservationDateSynchronizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

import static ch.admin.bit.jeap.governance.reactionobserver.dataimport.ImportOrder.REACTION_OBSERVER_COMPONENT_LAST_OBSERVATION_DATE_IMPORT_ORDER;

@Component
@Order(REACTION_OBSERVER_COMPONENT_LAST_OBSERVATION_DATE_IMPORT_ORDER)
@RequiredArgsConstructor
@Slf4j
public class ReactionObserverComponentLastObservationDateImporter implements DataSourceImporter {

    private final ReactionObserverConnector connector;
    private final ReactionObserverComponentLastObservationDateSynchronizer synchronizer;

    @Override
    public void importData() {
        log.info("Start synchronization with reaction observer");
        Map<String, LocalDate> componentLastObservationDates = connector.getAllComponentLastObservationDates();
        log.debug("Got model from reaction observer: {}", componentLastObservationDates);
        synchronizer.synchronizeModelWithReactionObserver(componentLastObservationDates);
        log.info("Finished synchronization with reaction observer");
    }
}
