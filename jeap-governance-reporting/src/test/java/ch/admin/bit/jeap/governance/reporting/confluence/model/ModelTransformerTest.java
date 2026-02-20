package ch.admin.bit.jeap.governance.reporting.confluence.model;

import ch.admin.bit.jeap.governance.domain.rule.State;
import ch.admin.bit.jeap.governance.reporting.preparation.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ModelTransformerTest {

    private static final LocalDate DAY = LocalDate.now();
    private static final ZonedDateTime TIMESTAMP = ZonedDateTime.now();

    @Test
    void toSystemScoreReportModel_ReportingSystemScore() {
        String systemPageSuffix = "systemPageSuffix";
        String componentPageSuffix = "componentPageSuffix";
        ModelTransformer transformer = new ModelTransformer(systemPageSuffix, componentPageSuffix);

        String systemName = "System A";
        String componentName = "System A";
        int systemScore = 85;
        int componentScore = 90;
        TrendIndicator systemTrend = TrendIndicator.UP;
        TrendIndicator componentTrend = TrendIndicator.DOWN;
        ReportingScore systemReportingScore = new ReportingScore(DAY, 90);
        ReportingScore componentReportingScore = new ReportingScore(DAY, 90);

        ReportingRuleState reportingRuleState = new ReportingRuleState("rule1", "Rule 1", "doc link", State.OK, "All good", TIMESTAMP);


        ReportingComponentScore reportingComponentScore = mock(ReportingComponentScore.class);
        when(reportingComponentScore.getComponentName()).thenReturn(componentName);
        when(reportingComponentScore.getLatestScore()).thenReturn(componentScore);
        when(reportingComponentScore.getScoringTrend()).thenReturn(componentTrend);
        when(reportingComponentScore.getScores()).thenReturn(List.of(componentReportingScore));
        when(reportingComponentScore.getRuleStates()).thenReturn(List.of(reportingRuleState));

        ReportingSystemScore reportingSystemScore = mock(ReportingSystemScore.class);
        when(reportingSystemScore.getSystemName()).thenReturn(systemName);
        when(reportingSystemScore.getLatestScore()).thenReturn(systemScore);
        when(reportingSystemScore.getScoringTrend()).thenReturn(systemTrend);
        when(reportingSystemScore.getScores()).thenReturn(List.of(systemReportingScore));
        when(reportingSystemScore.getComponentScores()).thenReturn(List.of(reportingComponentScore));


        SystemScoreReportModel actual = transformer.toConfluenceModel(reportingSystemScore);
        assertNotNull(actual);
        assertEquals(systemName, actual.getSystemName());
        assertEquals(systemPageSuffix, actual.getPageSuffix());
        assertEquals(systemScore, actual.getScore());
        assertEquals(Trend.UP, actual.getTrend());
        assertEquals(1, actual.getScores().size());
        assertEquals(systemReportingScore, actual.getScores().getFirst());
        assertEquals(1, actual.getComponentScores().size());
        ComponentScoreReportModel componentScoreReportModel = actual.getComponentScores().getFirst();
        assertEquals(componentName, componentScoreReportModel.getComponentName());
        assertEquals(componentPageSuffix, componentScoreReportModel.getPageSuffix());
        assertEquals(componentScore, componentScoreReportModel.getScore());
        assertEquals(Trend.DOWN, componentScoreReportModel.getTrend());
        assertEquals(1, componentScoreReportModel.getScores().size());
        assertEquals(componentReportingScore, componentScoreReportModel.getScores().getFirst());
        assertEquals(1, componentScoreReportModel.getRuleStates().size());
        RuleStateReportModel ruleStateReportModel = componentScoreReportModel.getRuleStates().getFirst();
        assertEquals(reportingRuleState.ruleId(), ruleStateReportModel.getRuleId());
        assertEquals(reportingRuleState.label(), ruleStateReportModel.getLabel());
        assertEquals(reportingRuleState.documentationLink(), ruleStateReportModel.getDocumentationLink());
        assertEquals(ch.admin.bit.jeap.governance.reporting.confluence.model.State.OK, ruleStateReportModel.getState());
        assertEquals(reportingRuleState.stateComment(), ruleStateReportModel.getStateComment());
        assertEquals(reportingRuleState.modifiedAt(), ruleStateReportModel.getModifiedAt());
    }

    @Test
    void toSystemScoreReportModel_ReportingRule() {
        String systemPageSuffix = "systemPageSuffix";
        String componentPageSuffix = "componentPageSuffix";
        ModelTransformer transformer = new ModelTransformer(systemPageSuffix, componentPageSuffix);

        ReportingRuleConformanceRate conformanceRate = mock(ReportingRuleConformanceRate.class);
        when(conformanceRate.getRate()).thenReturn(80);
        when(conformanceRate.getDay()).thenReturn(DAY);

        ReportingRuleSystemConformanceRate systemConformanceRate = mock(ReportingRuleSystemConformanceRate.class);
        when(systemConformanceRate.getSystemId()).thenReturn(1L);
        when(systemConformanceRate.getSystemName()).thenReturn("System A");
        when(systemConformanceRate.getLatestConformanceRate()).thenReturn(80);

        ReportingRuleNonCompliantComponent nonCompliantComponent = mock(ReportingRuleNonCompliantComponent.class);
        when(nonCompliantComponent.getSystemId()).thenReturn(1L);
        when(nonCompliantComponent.getSystemName()).thenReturn("System A");
        when(nonCompliantComponent.getComponentId()).thenReturn(1L);
        when(nonCompliantComponent.getComponentName()).thenReturn("Component A");
        when(nonCompliantComponent.getNonComplianceSince()).thenReturn(TIMESTAMP);


        String ruleId = "rule1";
        String ruleName = "Rule 1";
        List<ReportingRuleConformanceRate> conformanceRates = List.of(conformanceRate);
        List<ReportingRuleSystemConformanceRate> systemConformanceRates = List.of(systemConformanceRate);
        List<ReportingRuleNonCompliantComponent> nonCompliantComponents = List.of(nonCompliantComponent);

        ReportingRule reportingRule = mock(ReportingRule.class);
        when(reportingRule.getRuleId()).thenReturn(ruleId);
        when(reportingRule.getRuleName()).thenReturn(ruleName);
        when(reportingRule.getConformanceRateTrend()).thenReturn(TrendIndicator.UP);
        when(reportingRule.getLatestConformanceRate()).thenReturn(80);
        when(reportingRule.getConformanceRates()).thenReturn(conformanceRates);
        when(reportingRule.getSystemConformanceRates()).thenReturn(systemConformanceRates);
        when(reportingRule.getNonCompliantComponents()).thenReturn(nonCompliantComponents);


        RuleReportModel confluenceModel = transformer.toConfluenceModel(reportingRule);
        assertNotNull(confluenceModel);
        assertEquals(ruleId, confluenceModel.getRuleId());
        assertEquals(ruleName, confluenceModel.getName());
        assertEquals(80, confluenceModel.getConformanceRate());
        assertEquals(Trend.UP, confluenceModel.getTrend());
        assertEquals(1, confluenceModel.getSystems().size());
        RuleReportSystemModel ruleReportSystemModel = confluenceModel.getSystems().getFirst();
        assertEquals(systemConformanceRate.getSystemId(), ruleReportSystemModel.getId());
        assertEquals(systemConformanceRate.getSystemName(), ruleReportSystemModel.getName());
        assertEquals(systemConformanceRate.getLatestConformanceRate(), ruleReportSystemModel.getConformanceRate());
        assertEquals(1, confluenceModel.getNonCompliantComponents().size());
        RuleReportComponentModel ruleReportComponentModel = confluenceModel.getNonCompliantComponents().getFirst();
        assertEquals(nonCompliantComponent.getSystemName(), ruleReportComponentModel.getSystemName());
        assertEquals(nonCompliantComponent.getComponentId(), ruleReportComponentModel.getId());
        assertEquals(nonCompliantComponent.getComponentName(), ruleReportComponentModel.getName());
        assertEquals(nonCompliantComponent.getNonComplianceSince(), ruleReportComponentModel.getNonCompliantSince());
    }

    @Test
    void toConfluenceState() {
        ModelTransformer transformer = new ModelTransformer("systemPageSuffix", "componentPageSuffix");

        assertEquals(ch.admin.bit.jeap.governance.reporting.confluence.model.State.OK, transformer.toConfluenceState(State.OK));
        assertEquals(ch.admin.bit.jeap.governance.reporting.confluence.model.State.PAUSED, transformer.toConfluenceState(State.PAUSED));
        assertEquals(ch.admin.bit.jeap.governance.reporting.confluence.model.State.DISABLED, transformer.toConfluenceState(State.DISABLED));
        assertEquals(ch.admin.bit.jeap.governance.reporting.confluence.model.State.FAIL, transformer.toConfluenceState(State.FAIL));
    }

    @Test
    void toConfluenceTrend() {
        ModelTransformer transformer = new ModelTransformer("systemPageSuffix", "componentPageSuffix");

        assertEquals(Trend.UP, transformer.toConfluenceTrend(TrendIndicator.UP));
        assertEquals(Trend.DOWN, transformer.toConfluenceTrend(TrendIndicator.DOWN));
        assertEquals(Trend.NO_DATA, transformer.toConfluenceTrend(TrendIndicator.NO_DATA));
        assertEquals(Trend.EVEN, transformer.toConfluenceTrend(TrendIndicator.STABLE));
        assertEquals(Trend.UNKNOWN, transformer.toConfluenceTrend(TrendIndicator.UNKNOWN));
    }

}
