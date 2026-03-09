package ch.admin.bit.jeap.governance.domain.rule;

import ch.admin.bit.jeap.governance.domain.plugin.deletion.SystemDeletionListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SystemRuleConformanceRateSystemDeletionListener implements SystemDeletionListener {

    private final SystemRuleConformanceRateRepository repository;

    @Override
    public void preSystemDeletion(long systemId) {
        log.debug("Deleting SystemRuleConformanceRate entities related to system with ID: {}", systemId);
        repository.deleteAllBySystemId(systemId);
        log.debug("Deletion done");
    }
}
