package ch.admin.bit.jeap.governance.reactionobserver.synchronize;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.SystemComponentRepository;
import ch.admin.bit.jeap.governance.reactionobserver.domain.ReactionObserverComponentLastObservationDate;
import ch.admin.bit.jeap.governance.reactionobserver.domain.ReactionObserverComponentLastObservationDateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReactionObserverComponentLastObservationDateOneByOneSynchronizer {

    private final SystemComponentRepository systemComponentRepository;
    private final ReactionObserverComponentLastObservationDateRepository lastObservationDateRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void synchronize(String componentName, LocalDate date) {
        Optional<SystemComponent> systemComponentOptional = systemComponentRepository.findByName(componentName);
        if (systemComponentOptional.isEmpty()) {
            log.warn("Received componentName lastObservationDate for unknown system componentName '{}', skipping synchronization", componentName);
            return;
        }
        synchronize(systemComponentOptional.get(), date);
    }

    private void synchronize(SystemComponent systemComponent, LocalDate lastObservationDate) {
        Optional<ReactionObserverComponentLastObservationDate> entityOptional = lastObservationDateRepository.findByComponentName(systemComponent.getName());

        entityOptional.ifPresent(lastObservationDateRepository::delete);
        log.info("Creating new lastObservationDate for system component {}: {}", systemComponent.getName(), lastObservationDate);
        ReactionObserverComponentLastObservationDate componentLastObservationDate = ReactionObserverComponentLastObservationDate.builder()
                .systemComponent(systemComponent)
                .lastObservationDate(lastObservationDate)
                .build();
        lastObservationDateRepository.add(componentLastObservationDate);

    }
}
