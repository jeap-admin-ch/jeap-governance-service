package ch.admin.bit.jeap.governance.domain.rule;

import ch.admin.bit.jeap.governance.domain.SystemComponent;

import java.util.List;
import java.util.Optional;

public interface RuleStateRepository {

    Optional<RuleState> findBySystemComponentAndRuleId(SystemComponent systemComponent, RuleId ruleId);

    void saveAll(List<RuleState> ruleStates);

    List<RuleState> findAll();

    List<RuleState> findAllBySystemName(String systemName);

    List<NonCompliantComponentEntry> findNonCompliantSince();

    List<NonCompliantComponentEntry> findNonCompliantSinceByRuleId(String ruleId);

    default List<GracePeriodComponentEntry> findGracePeriodComponents() {
        return List.of();
    }

    default List<GracePeriodComponentEntry> findGracePeriodComponentsByRuleId(String ruleId) {
        return findGracePeriodComponents().stream()
                .filter(entry -> ruleId.equals(entry.getRuleId()))
                .toList();
    }

    void deleteAllBySystemComponentId(long systemComponentId);
}
