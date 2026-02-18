package ch.admin.bit.jeap.governance.domain.score;

import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.rule.RuleEvaluationService;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@Component
/** Orchestrates rule evaluation and score calculation for all components of a system. */
public class ScoringService {

    private final ComponentScoreCalculator componentScoreCalculator;
    private final ComponentScoreRepository componentScoreRepository;
    private final SystemScoreCalculator systemScoreCalculator;
    private final SystemScoreRepository systemScoreRepository;
    private final RuleEvaluationService ruleEvaluationService;

    @Timed("jeap.governance.service.scoring")
    @Transactional
    public void updateSystemScore(System system) {
        var day = LocalDate.now();

        List<ComponentScore> componentScores = system.getSystemComponents().stream()
                .map(systemComponent -> updateComponentScore(systemComponent, day))
                .toList();
        componentScoreRepository.saveOrReplaceAllForSystemAndDay(system, componentScores, day);

        var systemScore = systemScoreCalculator.calculateSystemScore(system, day, componentScores);
        systemScoreRepository.save(systemScore);
    }

    private ComponentScore updateComponentScore(SystemComponent systemComponent, LocalDate day) {
        var results = ruleEvaluationService.updateRuleStatesForComponent(systemComponent);
        return componentScoreCalculator.calculateComponentScore(systemComponent, day, results);
    }
}
