package ch.admin.bit.jeap.governance.domain.rule;

import ch.admin.bit.jeap.governance.domain.SystemComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Evaluates all applicable rules for a component and persists the resulting rule states.
 */
@Component
public class RuleEvaluationService {

    private final RuleEvaluator ruleEvaluator;
    private final RuleStateRepository ruleStateRepository;
    private final Clock clock;

    @Autowired
    public RuleEvaluationService(RuleEvaluator ruleEvaluator, RuleStateRepository ruleStateRepository) {
        this(ruleEvaluator, ruleStateRepository, Clock.systemDefaultZone());
    }

    RuleEvaluationService(RuleEvaluator ruleEvaluator, RuleStateRepository ruleStateRepository, Clock clock) {
        this.ruleEvaluator = ruleEvaluator;
        this.ruleStateRepository = ruleStateRepository;
        this.clock = clock;
    }

    public List<RuleEvaluationResult> updateRuleStatesForComponent(SystemComponent systemComponent) {
        List<RuleEvaluationResult> evaluatedResults = ruleEvaluator.evaluateRulesForComponent(systemComponent);
        List<RuleEvaluationResult> effectiveResults = new ArrayList<>(evaluatedResults.size());
        List<RuleState> ruleStates = new ArrayList<>(evaluatedResults.size());
        for (RuleEvaluationResult result : evaluatedResults) {
            ruleStates.add(updateRuleState(result, systemComponent, effectiveResults));
        }
        ruleStateRepository.saveAll(ruleStates);
        return effectiveResults;
    }

    private RuleState updateRuleState(RuleEvaluationResult result, SystemComponent systemComponent,
                                      List<RuleEvaluationResult> effectiveResults) {
        ZonedDateTime now = ZonedDateTime.now(clock);
        Optional<RuleState> existing = ruleStateRepository.findBySystemComponentAndRuleId(systemComponent, result.ruleId());
        RuleState ruleState = existing.orElseGet(() -> result.toRuleState(systemComponent));
        RuleEvaluationResult effectiveResult = applyViolationDelay(result, ruleState, now);
        ruleState.modify(effectiveResult.state(), effectiveResult.stateComment(), now);
        if (effectiveResult.state() == State.OK && ruleState.getViolationDetectedAt() != null) {
            ruleState.markViolationEvaluated(now);
        }
        effectiveResults.add(effectiveResult);
        return ruleState;
    }

    private static RuleEvaluationResult applyViolationDelay(RuleEvaluationResult result, RuleState ruleState,
                                                              ZonedDateTime now) {
        Duration delay = result.violationDelay();
        if (result.state() != State.FAIL || delay == null || delay.isZero() || delay.isNegative()) {
            ruleState.clearViolation();
            return result;
        }

        ruleState.startViolation(now);
        ZonedDateTime deadline = ruleState.getViolationDetectedAt().plus(delay);
        return now.isBefore(deadline) ? result.delayedUntil(deadline) : result;
    }
}
