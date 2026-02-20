package ch.admin.bit.jeap.governance.reporting.preparation;

import ch.admin.bit.jeap.db.tx.TransactionalReadReplica;
import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleInfo;
import ch.admin.bit.jeap.governance.domain.rule.RuleState;
import ch.admin.bit.jeap.governance.domain.score.ComponentScore;
import ch.admin.bit.jeap.governance.domain.score.SystemScore;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportingSystemsPreparation {

    private final ReportingDataAccess dataAccess;

    @TransactionalReadReplica
    @Timed("jeap.governance.service.reporting.systems.preparation")
    public List<ReportingSystemScore> prepareAllSystemsScores(LocalDate fromDay, LocalDate toDay) {
        List<SystemScore> systemScores = dataAccess.findAllSystemScoresByDayBetweenInclusive(fromDay, toDay);
        List<ComponentScore> componentScores = dataAccess.findAllComponentScoreByDayBetweenInclusive(fromDay, toDay);
        List<RuleInfo> activeRules = dataAccess.findAllActiveRuleInfos();
        List<RuleState> ruleStates = dataAccess.findAllRuleStates();

        return prepareAllSystemsScores(systemScores, componentScores, activeRules, ruleStates);
    }

    private List<ReportingSystemScore> prepareAllSystemsScores(List<SystemScore> systemScores, List<ComponentScore> componentScores, List<RuleInfo> activeRules, List<RuleState> ruleStates) {
        Map<Long, List<ReportingRuleState>> ruleStatesPerComponentId = prepareRuleStates(activeRules, ruleStates);
        Map<Long, Map<Long, ReportingComponentScore>> componentScoresPerSystem = prepareComponentsScores(componentScores, ruleStatesPerComponentId);
        Map<Long, ReportingSystemScore> systemScoreContainerPerSystem = prepareSystemScores(systemScores, componentScoresPerSystem);

        return new ArrayList<>(systemScoreContainerPerSystem.values());
    }

    private static Map<Long, Map<Long, ReportingComponentScore>> prepareComponentsScores(List<ComponentScore> componentScores, Map<Long, List<ReportingRuleState>> ruleStatesPerComponentId) {
        Map<Long, Map<Long, ReportingComponentScore>> componentScoresPerSystem = new HashMap<>();
        for (ComponentScore componentScore : componentScores) {
            SystemComponent systemComponent = componentScore.getSystemComponent();
            System system = systemComponent.getSystem();
            Long systemId = system.getId();
            componentScoresPerSystem
                    .computeIfAbsent(systemId, _ -> new HashMap<>())
                    .computeIfAbsent(systemComponent.getId(),
                            _ -> new ReportingComponentScore(
                                    systemComponent.getId(),
                                    systemComponent.getName(),
                                    ruleStatesPerComponentId.getOrDefault(systemComponent.getId(), new ArrayList<>())
                            )
                    )
                    .addScore(componentScore);
        }
        return componentScoresPerSystem;
    }

    private static Map<Long, List<ReportingRuleState>> prepareRuleStates(List<RuleInfo> activeRules, List<RuleState> ruleStates) {
        Map<Long, List<ReportingRuleState>> ruleStatesPerComponentId = new HashMap<>();
        Map<String, RuleInfo> ruleInfoByRuleId = activeRules.stream().collect(Collectors.toMap(ruleInfo -> ruleInfo.ruleId().id(), rm -> rm));
        for (RuleState ruleState : ruleStates) {

            RuleInfo ruleInfo = ruleInfoByRuleId.get(ruleState.getRuleId());
            if (ruleInfo == null) {
                log.warn("Could not find any rule metadata for rule id: {}, rule state: {}", ruleState.getRuleId(), ruleState);
                continue;
            }
            Long componentId = ruleState.getSystemComponent().getId();
            ReportingRuleState reportingRuleState = new ReportingRuleState(ruleState.getRuleId(), ruleInfo.label(), ruleInfo.documentationLink(),
                    ruleState.getState(), ruleState.getStateComment(), ruleState.getModifiedAt());
            ruleStatesPerComponentId
                    .computeIfAbsent(componentId, _ -> new ArrayList<>())
                    .add(reportingRuleState);
        }
        return ruleStatesPerComponentId;
    }

    private static Map<Long, ReportingSystemScore> prepareSystemScores(List<SystemScore> systemScores, Map<Long, Map<Long, ReportingComponentScore>> componentScoresPerSystem) {
        Map<Long, ReportingSystemScore> systemScoreContainerPerSystem = new HashMap<>();
        for (SystemScore systemScore : systemScores) {
            System system = systemScore.getSystem();
            Long systemId = system.getId();
            String systemName = system.getName();
            systemScoreContainerPerSystem
                    .computeIfAbsent(systemId, _ -> new ReportingSystemScore(systemId, systemName, componentScoresPerSystem.getOrDefault(systemId, new HashMap<>())))
                    .addScore(systemScore);
        }
        return systemScoreContainerPerSystem;
    }
}
