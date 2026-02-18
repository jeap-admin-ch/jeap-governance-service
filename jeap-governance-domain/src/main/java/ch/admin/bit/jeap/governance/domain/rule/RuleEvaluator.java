package ch.admin.bit.jeap.governance.domain.rule;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
/**
 * Evaluates rules for a component, respecting exemption states.
 */
class RuleEvaluator {

    private final RuleRepository ruleRepository;

    List<RuleEvaluationResult> evaluateRulesForComponent(SystemComponent systemComponent) {
        var ruleEvaluations = ruleRepository.getRulesToEvaluateForComponent(systemComponent);
        return ruleEvaluations.stream()
                .map(rule -> evaluate(rule, systemComponent))
                .toList();
    }

    private RuleEvaluationResult evaluate(RuleEvaluation ruleEvaluation, SystemComponent systemComponent) {
        return switch (ruleEvaluation.activationState()) {
            case ACTIVE -> evaluateRule(ruleEvaluation, systemComponent);
            case EXEMPTED -> RuleEvaluationResult.exempted(ruleEvaluation);
            case EXEMPTED_UNTIL -> RuleEvaluationResult.exemptedUntil(ruleEvaluation);
        };
    }

    private RuleEvaluationResult evaluateRule(RuleEvaluation ruleEvaluation, SystemComponent systemComponent) {
        var rule = ruleEvaluation.rule();
        return rule.evaluate(systemComponent, ruleEvaluation.ruleParameters());
    }
}
