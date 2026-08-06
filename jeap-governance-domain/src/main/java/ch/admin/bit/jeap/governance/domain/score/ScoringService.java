package ch.admin.bit.jeap.governance.domain.score;

import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.SystemRepository;
import ch.admin.bit.jeap.governance.domain.rule.RuleEvaluationResult;
import ch.admin.bit.jeap.governance.domain.rule.RuleEvaluationService;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static ch.admin.bit.jeap.governance.domain.ComponentType.isIgnoredForGovernance;

/**
 * Orchestrates rule evaluation and score calculation for all components of a system.
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class ScoringService {

    private final ComponentScoreCalculator componentScoreCalculator;
    private final ComponentScoreRepository componentScoreRepository;
    private final SystemScoreCalculator systemScoreCalculator;
    private final SystemScoreRepository systemScoreRepository;
    private final RuleEvaluationService ruleEvaluationService;
    private final SystemRepository systemRepository;

    @Timed("jeap.governance.service.scoring")
    @Transactional
    public List<RuleEvaluationResult> updateSystemScore(long systemId, LocalDate day) {
        System system = systemRepository.findById(systemId).orElseThrow();
        log.info("Updating system score for system {}", system.getName());
        List<RuleEvaluationResult> allResults = new ArrayList<>();

        List<ComponentScore> componentScores = system.getSystemComponents().stream()
                .filter(systemComponent -> !isIgnoredForGovernance(systemComponent.getType()))
                .map(systemComponent -> evaluateComponentScore(systemComponent, day, allResults))
                .toList();
        componentScoreRepository.saveOrReplaceAllForSystemAndDay(system, componentScores, day);

        var systemScore = systemScoreCalculator.calculateSystemScore(system, day, componentScores);
        systemScoreRepository.save(systemScore);

        return allResults;
    }

    private ComponentScore evaluateComponentScore(SystemComponent systemComponent, LocalDate day, List<RuleEvaluationResult> allResults) {
        var results = ruleEvaluationService.updateRuleStatesForComponent(systemComponent);
        allResults.addAll(results);
        return componentScoreCalculator.calculateComponentScore(systemComponent, day, results);
    }
}
