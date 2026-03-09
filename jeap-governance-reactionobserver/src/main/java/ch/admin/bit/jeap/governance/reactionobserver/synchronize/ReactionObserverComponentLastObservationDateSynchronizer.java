package ch.admin.bit.jeap.governance.reactionobserver.synchronize;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReactionObserverComponentLastObservationDateSynchronizer {

    private final ReactionObserverComponentLastObservationDateOneByOneSynchronizer oneByOneSynchronizer;

    public void synchronizeModelWithReactionObserver(Map<String, LocalDate> dates) {
        boolean hasException = false;
        for (Map.Entry<String, LocalDate> entry : dates.entrySet()) {
            try {
                oneByOneSynchronizer.synchronize(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                // Log and continue with next system to avoid blocking the whole synchronization in case of errors
                log.error("Error synchronizing ReactionObserver ComponentLastObservationDate for system component {}: {}. Proceeding import", entry.getKey(), e.getMessage(), e);
                hasException = true;
            }
        }
        if (hasException) {
            throw new ReactionObserverSynchronizeException("Errors occurred during ReactionObserver ComponentLastObservationDate synchronization. Check logs for details.");
        }
    }

}
