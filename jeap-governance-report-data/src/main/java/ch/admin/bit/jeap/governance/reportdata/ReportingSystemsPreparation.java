package ch.admin.bit.jeap.governance.reportdata;

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
import java.util.Optional;
import java.util.stream.Collectors;

import static ch.admin.bit.jeap.governance.domain.ComponentType.isIgnoredForGovernance;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportingSystemsPreparation {

    private final ReportingDataAccess dataAccess;

    @TransactionalReadReplica
    @Timed("jeap.governance.service.report-data.systems.preparation")
    public List<ReportingSystemScore> prepareAllSystemsScores(LocalDate fromDay, LocalDate toDay) {
        List<SystemScore> systemScores = dataAccess.findAllSystemScoresByDayBetweenInclusive(fromDay, toDay);
        List<ComponentScore> componentScores = dataAccess.findAllComponentScoreByDayBetweenInclusive(fromDay, toDay);
        List<RuleInfo> activeRules = dataAccess.findAllActiveRuleInfos();
        List<RuleState> ruleStates = dataAccess.findAllRuleStates();

        return prepareAllSystemsScores(systemScores, componentScores, activeRules, ruleStates);
    }

    /**
     * Prepares one system as {@link #prepareAllSystemsScores(LocalDate, LocalDate)} does, loading only its data.
     *
     * @return the system, or empty if it has no score in the period
     */
    @TransactionalReadReplica
    @Timed("jeap.governance.service.report-data.system.preparation")
    public Optional<ReportingSystemScore> prepareSystemScores(String systemName, LocalDate fromDay, LocalDate toDay) {
        List<SystemScore> systemScores = dataAccess.findSystemScoresBySystemNameAndDayBetweenInclusive(systemName, fromDay, toDay);
        if (systemScores.isEmpty()) {
            return Optional.empty();
        }
        List<ComponentScore> componentScores = dataAccess.findComponentScoresBySystemNameAndDayBetweenInclusive(systemName, fromDay, toDay);
        List<RuleInfo> activeRules = dataAccess.findAllActiveRuleInfos();
        List<RuleState> ruleStates = dataAccess.findRuleStatesBySystemName(systemName);

        return prepareAllSystemsScores(systemScores, componentScores, activeRules, ruleStates).stream().findFirst();
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
            if (isIgnoredForGovernance(systemComponent.getType())) {
                continue;
            }
            System system = systemComponent.getSystem();
            Long systemId = system.getId();
            componentScoresPerSystem
                    .computeIfAbsent(systemId, _ -> new HashMap<>())
                    .computeIfAbsent(systemComponent.getId(),
                            _ -> new ReportingComponentScore(
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
            if (!isIgnoredForGovernance(ruleState.getSystemComponent().getType())) {
                addRuleState(ruleStatesPerComponentId, ruleInfoByRuleId, ruleState);
            }
        }
        return ruleStatesPerComponentId;
    }

    private static void addRuleState(Map<Long, List<ReportingRuleState>> ruleStatesPerComponentId,
                                     Map<String, RuleInfo> ruleInfoByRuleId,
                                     RuleState ruleState) {
        RuleInfo ruleInfo = ruleInfoByRuleId.get(ruleState.getRuleId());
        if (ruleInfo == null) {
            log.warn("Could not find any rule metadata for rule id: {}, rule state: {}", ruleState.getRuleId(), ruleState);
            return;
        }

        Long componentId = ruleState.getSystemComponent().getId();
        ReportingRuleState reportingRuleState = new ReportingRuleState(ruleState.getRuleId(), ruleInfo.label(),
                ruleState.getState(), ruleState.getStateComment(), ruleState.getModifiedAt());
        ruleStatesPerComponentId
                .computeIfAbsent(componentId, _ -> new ArrayList<>())
                .add(reportingRuleState);
    }

    private static Map<Long, ReportingSystemScore> prepareSystemScores(List<SystemScore> systemScores, Map<Long, Map<Long, ReportingComponentScore>> componentScoresPerSystem) {
        Map<Long, ReportingSystemScore> systemScoreContainerPerSystem = new HashMap<>();
        for (SystemScore systemScore : systemScores) {
            System system = systemScore.getSystem();
            Long systemId = system.getId();
            String systemName = system.getName();
            systemScoreContainerPerSystem
                    .computeIfAbsent(systemId, _ -> new ReportingSystemScore(systemName, componentScoresPerSystem.getOrDefault(systemId, new HashMap<>())))
                    .addScore(systemScore);
        }
        return systemScoreContainerPerSystem;
    }
}
