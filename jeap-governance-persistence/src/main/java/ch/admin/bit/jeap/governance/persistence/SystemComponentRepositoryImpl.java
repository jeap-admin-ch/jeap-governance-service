package ch.admin.bit.jeap.governance.persistence;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.SystemComponentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class SystemComponentRepositoryImpl implements SystemComponentRepository {

    private final JpaComponentRepository jpaComponentRepository;

    @Override
    @Transactional
    public void deleteById(UUID systemComponentId) {
        Optional<SystemComponent> byId = jpaComponentRepository.findById(systemComponentId);
        if (byId.isEmpty()) {
            log.info("SystemComponent with id {} not found for deletion.", systemComponentId);
            return;
        }
        SystemComponent systemComponent = byId.get();
        log.info("Deleting SystemComponent: {}", systemComponent);
        systemComponent.getSystem().deleteSystemComponent(systemComponent);
    }

    @Override
    public Optional<SystemComponent> findByName(String componentName) {
        return jpaComponentRepository.findByName(componentName);
    }
}
