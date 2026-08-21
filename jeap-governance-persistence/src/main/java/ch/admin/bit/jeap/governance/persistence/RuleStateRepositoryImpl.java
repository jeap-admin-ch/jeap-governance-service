package ch.admin.bit.jeap.governance.persistence;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.rule.GracePeriodComponentEntry;
import ch.admin.bit.jeap.governance.domain.rule.NonCompliantComponentEntry;
import ch.admin.bit.jeap.governance.domain.rule.RuleId;
import ch.admin.bit.jeap.governance.domain.rule.RuleState;
import ch.admin.bit.jeap.governance.domain.rule.RuleStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RuleStateRepositoryImpl implements RuleStateRepository {

    private final JpaRuleStateRepository jpaRuleStateRepository;

    @Override
    public Optional<RuleState> findBySystemComponentAndRuleId(SystemComponent systemComponent, RuleId ruleId) {
        return jpaRuleStateRepository.findBySystemComponentAndRuleId(systemComponent, ruleId.id());
    }

    @Override
    public void saveAll(List<RuleState> ruleStates) {
        jpaRuleStateRepository.saveAll(ruleStates);
    }

    @Override
    public List<RuleState> findAll() {
        return jpaRuleStateRepository.findAll();
    }

    @Override
    public List<NonCompliantComponentEntry> findNonCompliantSince() {
        return jpaRuleStateRepository.findNonCompliantSince();
    }

    @Override
    public List<GracePeriodComponentEntry> findGracePeriodComponents() {
        return jpaRuleStateRepository.findGracePeriodComponents();
    }

    @Override
    public void deleteAllBySystemComponentId(long systemComponentId) {
        jpaRuleStateRepository.deleteAllBySystemComponentId(systemComponentId);
    }
}
