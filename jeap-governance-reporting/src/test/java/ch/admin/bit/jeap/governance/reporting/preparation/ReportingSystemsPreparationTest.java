package ch.admin.bit.jeap.governance.reporting.preparation;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleInfo;
import ch.admin.bit.jeap.governance.domain.rule.RuleId;
import ch.admin.bit.jeap.governance.domain.rule.RuleState;
import ch.admin.bit.jeap.governance.domain.rule.State;
import ch.admin.bit.jeap.governance.domain.score.ComponentScore;
import ch.admin.bit.jeap.governance.domain.score.SystemScore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportingSystemsPreparationTest {

    private static final System SYSTEM_A = System.builder().name("System A").systemComponents(List.of()).build();
    private static final SystemComponent COMPONENT_A_1 = SystemComponent.builder().name("Component A1").type(ComponentType.BACKEND_SERVICE).build();
    private static final SystemComponent COMPONENT_A_2 = SystemComponent.builder().name("Component A2").type(ComponentType.BACKEND_SERVICE).build();
    private static final RuleInfo RULE_INFO_1 = new RuleInfo(new RuleId("Rule_1"), "Rule 1 label", "Rule 1 link");
    private static final RuleInfo RULE_INFO_2 = new RuleInfo(new RuleId("Rule_2"), "Rule 2 label", "Rule 2 link");

    private static final LocalDate DAY1 = LocalDate.now().minusDays(2);
    private static final LocalDate DAY2 = LocalDate.now().minusDays(1);
    private static final LocalDate DAY3 = LocalDate.now();

    static {
        SYSTEM_A.addSystemComponent(COMPONENT_A_1);
        SYSTEM_A.addSystemComponent(COMPONENT_A_2);
    }

    @Mock
    private ReportingDataAccess dataAccess;

    @InjectMocks
    private ReportingSystemsPreparation preparation;

    @Test
    void testPrepareSystemsScoreReportModel_returnScores_ifOnlySystemScoreSet() {
        List<SystemScore> systemScores = List.of(createSystemScore(SYSTEM_A, DAY2, 90), createSystemScore(SYSTEM_A, DAY3, 95), createSystemScore(SYSTEM_A, DAY1, 80));
        List<ComponentScore> componentScores = List.of();
        List<RuleInfo> activeRules = List.of();
        List<RuleState> ruleStates = List.of();
        when(dataAccess.findAllSystemScoresByDayBetweenInclusive(DAY1, DAY3)).thenReturn(systemScores);
        when(dataAccess.findAllComponentScoreByDayBetweenInclusive(DAY1, DAY3)).thenReturn(componentScores);
        when(dataAccess.findAllActiveRuleInfos()).thenReturn(activeRules);
        when(dataAccess.findAllRuleStates()).thenReturn(ruleStates);

        List<ReportingSystemScore> reportingSystemScores = preparation.prepareAllSystemsScores(DAY1, DAY3);

        assertNotNull(reportingSystemScores);
        assertEquals(1, reportingSystemScores.size());
        ReportingSystemScore score = reportingSystemScores.getFirst();
        List<ReportingScore> scores = score.getScores();
        assertEquals(3, scores.size());
        assertEquals(DAY1, scores.get(0).getDay());
        assertEquals(80, scores.get(0).getScore());
        assertEquals(DAY2, scores.get(1).getDay());
        assertEquals(90, scores.get(1).getScore());
        assertEquals(DAY3, scores.get(2).getDay());
    }

    @Test
    void testPrepareSystemsScoreReportModel_returnScores_ifSystemAndComponentScoreSet() {
        List<SystemScore> systemScores = List.of(createSystemScore(SYSTEM_A, DAY2, 90), createSystemScore(SYSTEM_A, DAY3, 95), createSystemScore(SYSTEM_A, DAY1, 80));
        List<ComponentScore> componentScores = List.of(createComponentScore(COMPONENT_A_1, DAY2, 50), createComponentScore(COMPONENT_A_1, DAY3, 60), createComponentScore(COMPONENT_A_1, DAY1, 40));
        List<RuleInfo> activeRules = List.of();
        List<RuleState> ruleStates = List.of();
        when(dataAccess.findAllSystemScoresByDayBetweenInclusive(DAY1, DAY3)).thenReturn(systemScores);
        when(dataAccess.findAllComponentScoreByDayBetweenInclusive(DAY1, DAY3)).thenReturn(componentScores);
        when(dataAccess.findAllActiveRuleInfos()).thenReturn(activeRules);
        when(dataAccess.findAllRuleStates()).thenReturn(ruleStates);

        List<ReportingSystemScore> reportingSystemScores = preparation.prepareAllSystemsScores(DAY1, DAY3);

        assertNotNull(reportingSystemScores);
        assertEquals(1, reportingSystemScores.size());
        ReportingSystemScore systemScore = reportingSystemScores.getFirst();
        List<ReportingScore> scores = systemScore.getScores();
        assertEquals(3, scores.size());
        assertEquals(1, systemScore.getComponentScores().size());
        ReportingComponentScore componentScore = systemScore.getComponentScores().getFirst();
        List<ReportingScore> componentScoresResult = componentScore.getScores();
        assertEquals(3, componentScoresResult.size());
        assertEquals(DAY1, componentScoresResult.get(0).getDay());
        assertEquals(40, componentScoresResult.get(0).getScore());
        assertEquals(DAY2, componentScoresResult.get(1).getDay());
        assertEquals(50, componentScoresResult.get(1).getScore());
    }

    @Test
    void testPrepareSystemsScoreReportModel_returnScores_ifSystemComponentScoreAndRulesSet() {
        List<SystemScore> systemScores = List.of(createSystemScore(SYSTEM_A, DAY2, 90), createSystemScore(SYSTEM_A, DAY3, 95), createSystemScore(SYSTEM_A, DAY1, 80));
        List<ComponentScore> componentScores = List.of(createComponentScore(COMPONENT_A_1, DAY2, 50), createComponentScore(COMPONENT_A_1, DAY3, 60), createComponentScore(COMPONENT_A_1, DAY1, 40));
        List<RuleInfo> activeRules = List.of(RULE_INFO_1, RULE_INFO_2);
        List<RuleState> ruleStates = List.of(createRuleState(COMPONENT_A_1, RULE_INFO_1), createRuleState(COMPONENT_A_1, RULE_INFO_2));
        when(dataAccess.findAllSystemScoresByDayBetweenInclusive(DAY1, DAY3)).thenReturn(systemScores);
        when(dataAccess.findAllComponentScoreByDayBetweenInclusive(DAY1, DAY3)).thenReturn(componentScores);
        when(dataAccess.findAllActiveRuleInfos()).thenReturn(activeRules);
        when(dataAccess.findAllRuleStates()).thenReturn(ruleStates);

        List<ReportingSystemScore> reportingSystemScores = preparation.prepareAllSystemsScores(DAY1, DAY3);

        assertNotNull(reportingSystemScores);
        assertEquals(1, reportingSystemScores.size());
        ReportingSystemScore systemScore = reportingSystemScores.getFirst();
        List<ReportingScore> scores = systemScore.getScores();
        assertEquals(3, scores.size());
        assertEquals(1, systemScore.getComponentScores().size());
        ReportingComponentScore componentScore = systemScore.getComponentScores().getFirst();
        List<ReportingScore> componentScoresResult = componentScore.getScores();
        assertEquals(3, componentScoresResult.size());
        assertEquals(2, componentScore.getRuleStates().size());

    }

    @Test
    void testPrepareSystemsScoreReportModel_returnEmptyOutput_ifInputEmpty() {
        List<SystemScore> systemScores = List.of();
        List<ComponentScore> componentScores = List.of();
        List<RuleInfo> activeRules = List.of();
        List<RuleState> ruleStates = List.of();
        when(dataAccess.findAllSystemScoresByDayBetweenInclusive(DAY1, DAY3)).thenReturn(systemScores);
        when(dataAccess.findAllComponentScoreByDayBetweenInclusive(DAY1, DAY3)).thenReturn(componentScores);
        when(dataAccess.findAllActiveRuleInfos()).thenReturn(activeRules);
        when(dataAccess.findAllRuleStates()).thenReturn(ruleStates);

        List<ReportingSystemScore> reportingSystemScores = preparation.prepareAllSystemsScores(DAY1, DAY3);

        assertNotNull(reportingSystemScores);
        assertTrue(reportingSystemScores.isEmpty());
    }

    private SystemScore createSystemScore(System system, LocalDate day, int score) {
        return SystemScore.builder()
                .system(system)
                .day(day)
                .score(score)
                .build();
    }

    private ComponentScore createComponentScore(SystemComponent component, LocalDate day, int score) {
        return ComponentScore.builder()
                .systemComponent(component)
                .day(day)
                .score(score)
                .build();
    }

    private RuleState createRuleState(SystemComponent systemComponent, RuleInfo ruleMetadata) {
        return RuleState.builder()
                .ruleId(ruleMetadata.ruleId())
                .state(State.OK)
                .ruleStateComment("No comment" + ruleMetadata.label())
                .systemComponent(systemComponent)
                .build();
    }
}
