package ch.admin.bit.jeap.governance.domain.rule;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
/**
 * Evaluates all applicable rules for a component and persists the resulting rule states.
 */
public class RuleEvaluationService {

    private final RuleEvaluator ruleEvaluator;
    private final RuleStateRepository ruleStateRepository;

    public List<RuleEvaluationResult> updateRuleStatesForComponent(SystemComponent systemComponent) {
        List<RuleEvaluationResult> results = ruleEvaluator.evaluateRulesForComponent(systemComponent);
        saveOrUpdateRuleStates(results, systemComponent);
        return results;
    }

    private void saveOrUpdateRuleStates(List<RuleEvaluationResult> results, SystemComponent systemComponent) {
        List<RuleState> ruleStates = results.stream()
                .map(result -> getOrCreateRuleState(result, systemComponent))
                .toList();
        ruleStateRepository.saveAll(ruleStates);
    }

    private RuleState getOrCreateRuleState(RuleEvaluationResult result, SystemComponent systemComponent) {
        Optional<RuleState> existing = ruleStateRepository.findBySystemComponentAndRuleId(systemComponent, result.ruleId());
        return existing
                .map(rs -> {
                    rs.modify(result.state(), result.stateComment());
                    return rs;
                })
                .orElseGet(() -> result.toRuleState(systemComponent));
    }
}
