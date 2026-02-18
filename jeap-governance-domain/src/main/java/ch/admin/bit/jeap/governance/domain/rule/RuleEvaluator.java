package ch.admin.bit.jeap.governance.domain.rule;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Evaluates rules for a component, respecting exemption states.
 */
@Component
@RequiredArgsConstructor
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
            case EXEMPTED -> RuleEvaluationResult.exempted(ruleEvaluation.rule().metadata().ruleId());
            case EXEMPTED_UNTIL -> RuleEvaluationResult.exemptedUntil(ruleEvaluation.rule().metadata().ruleId());
        };
    }

    private RuleEvaluationResult evaluateRule(RuleEvaluation ruleEvaluation, SystemComponent systemComponent) {
        var rule = ruleEvaluation.rule();
        var result = rule.evaluate(systemComponent, ruleEvaluation.ruleParameters());
        return new RuleEvaluationResult(rule.metadata().ruleId(), result.state(), result.stateComment());
    }
}
