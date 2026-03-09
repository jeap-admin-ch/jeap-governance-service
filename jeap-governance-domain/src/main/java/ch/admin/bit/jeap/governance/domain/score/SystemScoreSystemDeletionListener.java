package ch.admin.bit.jeap.governance.domain.score;

import ch.admin.bit.jeap.governance.domain.plugin.deletion.SystemDeletionListener;
import ch.admin.bit.jeap.governance.domain.rule.SystemRuleConformanceRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SystemScoreSystemDeletionListener implements SystemDeletionListener {

    private final SystemScoreRepository repository;

    @Override
    public void preSystemDeletion(long systemId) {
        log.debug("Deleting SystemScore entities related to system with ID: {}", systemId);
        repository.deleteAllBySystemId(systemId);
        log.debug("Deletion done");
    }
}
