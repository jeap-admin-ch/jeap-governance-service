package ch.admin.bit.jeap.governance.domain.rule;

import ch.admin.bit.jeap.governance.domain.SystemComponent;

import java.util.List;
import java.util.Optional;

public interface RuleStateRepository {

    Optional<RuleState> findBySystemComponentAndRuleId(SystemComponent systemComponent, RuleId ruleId);

    void saveAll(List<RuleState> ruleStates);
}
