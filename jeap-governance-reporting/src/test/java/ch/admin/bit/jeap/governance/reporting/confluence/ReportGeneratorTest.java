package ch.admin.bit.jeap.governance.reporting.confluence;

import ch.admin.bit.jeap.governance.domain.rule.State;
import ch.admin.bit.jeap.governance.reporting.ReportingProperties;
import ch.admin.bit.jeap.governance.reporting.confluence.model.ComponentScoreReportModel;
import ch.admin.bit.jeap.governance.reporting.confluence.model.RuleReportModel;
import ch.admin.bit.jeap.governance.reporting.confluence.model.RulesReportModel;
import ch.admin.bit.jeap.governance.reporting.confluence.model.SystemScoreReportModel;
import ch.admin.bit.jeap.governance.reporting.confluence.model.SystemsScoreReportModel;
import ch.admin.bit.jeap.governance.reporting.preparation.ReportingComponentScore;
import ch.admin.bit.jeap.governance.reporting.preparation.ReportingRule;
import ch.admin.bit.jeap.governance.reporting.preparation.ReportingRuleState;
import ch.admin.bit.jeap.governance.reporting.preparation.ReportingScore;
import ch.admin.bit.jeap.governance.reporting.preparation.ReportingSystemScore;
import ch.admin.bit.jeap.governance.reporting.preparation.TrendIndicator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportGeneratorTest {

    private static final String ROOT_PAGE_NAME = "rootPageName";
    private static final String ROOT_PAGE_ID = "rootPageId";
    private static final String SYSTEMS_PAGE_CONTENT = "Systems page content";
    private static final String SYSTEMS_PAGE_ID = "systemsPageId";
    private static final String SYSTEM_PAGE_CONTENT_1 = "System page content 1";
    private static final String SYSTEM_PAGE_CONTENT_2 = "System page content 2";
    private static final String SYSTEM_PAGE_CONTENT_3 = "System page content 3";
    private static final String SYSTEM_PAGE_ID_1 = "systemPageId1";
    private static final String SYSTEM_PAGE_ID_2 = "systemPageId2";
    private static final String SYSTEM_PAGE_ID_3 = "systemPageId3";

    private static final String COMPONENT_PAGE_CONTENT_1 = "Component page content 1";
    private static final String COMPONENT_PAGE_CONTENT_2 = "Component page content 2";
    private static final String COMPONENT_PAGE_CONTENT_3 = "Component page content 3";

    private static final String COMPONENT_PAGE_ID_1 = "componentPageId1";
    private static final String COMPONENT_PAGE_ID_2 = "componentPageId2";
    private static final String COMPONENT_PAGE_ID_3 = "componentPageId3";

    private static final String RULES_PAGE_CONTENT = "Rules page content";
    private static final String RULES_PAGE_ID = "rulesPageId";

    private static final String RULE_PAGE_CONTENT_A = "Rule page content A";
    private static final String RULE_PAGE_CONTENT_B = "Rule page content B";
    private static final String RULE_PAGE_CONTENT_C = "Rule page content C";

    private static final String RULE_PAGE_ID_A = "rulePageIdA";
    private static final String RULE_PAGE_ID_B = "rulePageIdB";
    private static final String RULE_PAGE_ID_C = "rulePageIdC";

    @Mock
    private ConfluenceAdapter confluenceAdapter;
    @Mock
    private TemplateRenderer templateRenderer;
    @Mock
    private ReportingProperties properties;

    @InjectMocks
    private ReportGenerator reportGenerator;

    @Test
    void testGenerateSystemsReport() {
        when(properties.getConfluenceRootPageName()).thenReturn(ROOT_PAGE_NAME);
        when(confluenceAdapter.getPageByName(ROOT_PAGE_NAME)).thenReturn(ROOT_PAGE_ID);

        ArgumentCaptor<SystemsScoreReportModel> systemsScoreReportModelArgumentCaptor = ArgumentCaptor.forClass(SystemsScoreReportModel.class);
        when(templateRenderer.renderSystemsScorePage(systemsScoreReportModelArgumentCaptor.capture())).thenReturn(SYSTEMS_PAGE_CONTENT);
        when(confluenceAdapter.addOrUpdatePageUnderAncestor(ROOT_PAGE_ID, "System Scores", SYSTEMS_PAGE_CONTENT)).thenReturn(SYSTEMS_PAGE_ID);

        ReportingSystemScore systemScore1 = createSystemScore("System 1", 90);
        ReportingSystemScore systemScore2 = createSystemScore("System 2", 100);
        ReportingSystemScore systemScore3 = createSystemScore("System 3", 90);

        ArgumentCaptor<SystemScoreReportModel> systemScoreReportModelArgumentCaptor = ArgumentCaptor.forClass(SystemScoreReportModel.class);
        when(templateRenderer.renderSystemScorePage(systemScoreReportModelArgumentCaptor.capture())).thenReturn(SYSTEM_PAGE_CONTENT_2, SYSTEM_PAGE_CONTENT_1, SYSTEM_PAGE_CONTENT_3);
        when(confluenceAdapter.addOrUpdatePageUnderAncestor(SYSTEMS_PAGE_ID, "System 1 (System scores)", SYSTEM_PAGE_CONTENT_1)).thenReturn(SYSTEM_PAGE_ID_1);
        when(confluenceAdapter.addOrUpdatePageUnderAncestor(SYSTEMS_PAGE_ID, "System 2 (System scores)", SYSTEM_PAGE_CONTENT_2)).thenReturn(SYSTEM_PAGE_ID_2);
        when(confluenceAdapter.addOrUpdatePageUnderAncestor(SYSTEMS_PAGE_ID, "System 3 (System scores)", SYSTEM_PAGE_CONTENT_3)).thenReturn(SYSTEM_PAGE_ID_3);

        ArgumentCaptor<ComponentScoreReportModel> componentScoreReportModelArgumentCaptor = ArgumentCaptor.forClass(ComponentScoreReportModel.class);
        when(templateRenderer.renderComponentScorePage(componentScoreReportModelArgumentCaptor.capture())).thenReturn(COMPONENT_PAGE_CONTENT_2, COMPONENT_PAGE_CONTENT_1, COMPONENT_PAGE_CONTENT_3);
        when(confluenceAdapter.addOrUpdatePageUnderAncestor(SYSTEM_PAGE_ID_1, "System 1 Component A (Component scores)", COMPONENT_PAGE_CONTENT_1)).thenReturn(COMPONENT_PAGE_ID_1);
        when(confluenceAdapter.addOrUpdatePageUnderAncestor(SYSTEM_PAGE_ID_2, "System 2 Component A (Component scores)", COMPONENT_PAGE_CONTENT_2)).thenReturn(COMPONENT_PAGE_ID_2);
        when(confluenceAdapter.addOrUpdatePageUnderAncestor(SYSTEM_PAGE_ID_3, "System 3 Component A (Component scores)", COMPONENT_PAGE_CONTENT_3)).thenReturn(COMPONENT_PAGE_ID_3);

        List<ReportingSystemScore> systemScores = List.of(systemScore3, systemScore1, systemScore2);
        reportGenerator.generateSystemsReport(systemScores, true);

        List<SystemsScoreReportModel> systemsScoreReportModels = systemsScoreReportModelArgumentCaptor.getAllValues();
        assertEquals(1, systemsScoreReportModels.size());
        SystemsScoreReportModel systemsScoreReportModel = systemsScoreReportModels.getFirst();
        assertEquals("System Scores overview", systemsScoreReportModel.getTitle());

        List<SystemScoreReportModel> systemScoreReportModels = systemScoreReportModelArgumentCaptor.getAllValues();
        assertEquals(3, systemScoreReportModels.size());
        SystemScoreReportModel systemScoreReportModel1 = systemScoreReportModels.get(0);
        assertEquals("System 2", systemScoreReportModel1.getSystemName());
        SystemScoreReportModel systemScoreReportModel2 = systemScoreReportModels.get(1);
        assertEquals("System 1", systemScoreReportModel2.getSystemName());
        SystemScoreReportModel systemScoreReportModel3 = systemScoreReportModels.get(2);
        assertEquals("System 3", systemScoreReportModel3.getSystemName());

        verify(confluenceAdapter).deleteOrphanPages(SYSTEMS_PAGE_ID, Set.of(SYSTEM_PAGE_ID_1, SYSTEM_PAGE_ID_2, SYSTEM_PAGE_ID_3, COMPONENT_PAGE_ID_1, COMPONENT_PAGE_ID_2, COMPONENT_PAGE_ID_3));
    }

    @Test
    void testGenerateSystemsReport_withoutOrphanCleanup() {
        when(properties.getConfluenceRootPageName()).thenReturn(ROOT_PAGE_NAME);
        when(confluenceAdapter.getPageByName(ROOT_PAGE_NAME)).thenReturn(ROOT_PAGE_ID);

        ArgumentCaptor<SystemsScoreReportModel> systemsScoreReportModelArgumentCaptor = ArgumentCaptor.forClass(SystemsScoreReportModel.class);
        when(templateRenderer.renderSystemsScorePage(systemsScoreReportModelArgumentCaptor.capture())).thenReturn(SYSTEMS_PAGE_CONTENT);
        when(confluenceAdapter.addOrUpdatePageUnderAncestor(ROOT_PAGE_ID, "System Scores", SYSTEMS_PAGE_CONTENT)).thenReturn(SYSTEMS_PAGE_ID);

        ReportingSystemScore systemScore1 = createSystemScore("System 1", 90);
        ReportingSystemScore systemScore2 = createSystemScore("System 2", 100);
        ReportingSystemScore systemScore3 = createSystemScore("System 3", 90);

        ArgumentCaptor<SystemScoreReportModel> systemScoreReportModelArgumentCaptor = ArgumentCaptor.forClass(SystemScoreReportModel.class);
        when(templateRenderer.renderSystemScorePage(systemScoreReportModelArgumentCaptor.capture())).thenReturn(SYSTEM_PAGE_CONTENT_2, SYSTEM_PAGE_CONTENT_1, SYSTEM_PAGE_CONTENT_3);
        when(confluenceAdapter.addOrUpdatePageUnderAncestor(SYSTEMS_PAGE_ID, "System 1 (System scores)", SYSTEM_PAGE_CONTENT_1)).thenReturn(SYSTEM_PAGE_ID_1);
        when(confluenceAdapter.addOrUpdatePageUnderAncestor(SYSTEMS_PAGE_ID, "System 2 (System scores)", SYSTEM_PAGE_CONTENT_2)).thenReturn(SYSTEM_PAGE_ID_2);
        when(confluenceAdapter.addOrUpdatePageUnderAncestor(SYSTEMS_PAGE_ID, "System 3 (System scores)", SYSTEM_PAGE_CONTENT_3)).thenReturn(SYSTEM_PAGE_ID_3);

        ArgumentCaptor<ComponentScoreReportModel> componentScoreReportModelArgumentCaptor = ArgumentCaptor.forClass(ComponentScoreReportModel.class);
        when(templateRenderer.renderComponentScorePage(componentScoreReportModelArgumentCaptor.capture())).thenReturn(COMPONENT_PAGE_CONTENT_2, COMPONENT_PAGE_CONTENT_1, COMPONENT_PAGE_CONTENT_3);
        when(confluenceAdapter.addOrUpdatePageUnderAncestor(SYSTEM_PAGE_ID_1, "System 1 Component A (Component scores)", COMPONENT_PAGE_CONTENT_1)).thenReturn(COMPONENT_PAGE_ID_1);
        when(confluenceAdapter.addOrUpdatePageUnderAncestor(SYSTEM_PAGE_ID_2, "System 2 Component A (Component scores)", COMPONENT_PAGE_CONTENT_2)).thenReturn(COMPONENT_PAGE_ID_2);
        when(confluenceAdapter.addOrUpdatePageUnderAncestor(SYSTEM_PAGE_ID_3, "System 3 Component A (Component scores)", COMPONENT_PAGE_CONTENT_3)).thenReturn(COMPONENT_PAGE_ID_3);

        List<ReportingSystemScore> systemScores = List.of(systemScore3, systemScore1, systemScore2);
        reportGenerator.generateSystemsReport(systemScores, false);

        List<SystemsScoreReportModel> systemsScoreReportModels = systemsScoreReportModelArgumentCaptor.getAllValues();
        assertEquals(1, systemsScoreReportModels.size());
        SystemsScoreReportModel systemsScoreReportModel = systemsScoreReportModels.getFirst();
        assertEquals("System Scores overview", systemsScoreReportModel.getTitle());

        List<SystemScoreReportModel> systemScoreReportModels = systemScoreReportModelArgumentCaptor.getAllValues();
        assertEquals(3, systemScoreReportModels.size());
        SystemScoreReportModel systemScoreReportModel1 = systemScoreReportModels.get(0);
        assertEquals("System 2", systemScoreReportModel1.getSystemName());
        SystemScoreReportModel systemScoreReportModel2 = systemScoreReportModels.get(1);
        assertEquals("System 1", systemScoreReportModel2.getSystemName());
        SystemScoreReportModel systemScoreReportModel3 = systemScoreReportModels.get(2);
        assertEquals("System 3", systemScoreReportModel3.getSystemName());

        verify(confluenceAdapter, never()).deleteOrphanPages(anyString(), anySet());
    }

    @Test
    void testGenerateRulesReport() {
        when(properties.getConfluenceRootPageName()).thenReturn(ROOT_PAGE_NAME);
        when(confluenceAdapter.getPageByName(ROOT_PAGE_NAME)).thenReturn(ROOT_PAGE_ID);

        // Input is unsorted: B, A, C — expect alphabetical output: A, B, C
        ReportingRule ruleB = createReportingRule("rule-2", "Rule B");
        ReportingRule ruleA = createReportingRule("rule-1", "Rule A");
        ReportingRule ruleC = createReportingRule("rule-3", "Rule C");

        ArgumentCaptor<RulesReportModel> rulesReportModelCaptor = ArgumentCaptor.forClass(RulesReportModel.class);
        when(templateRenderer.renderRulesPage(rulesReportModelCaptor.capture())).thenReturn(RULES_PAGE_CONTENT);
        when(confluenceAdapter.addOrUpdatePageUnderAncestor(ROOT_PAGE_ID, "Rules", RULES_PAGE_CONTENT)).thenReturn(RULES_PAGE_ID);

        ArgumentCaptor<RuleReportModel> ruleReportModelCaptor = ArgumentCaptor.forClass(RuleReportModel.class);
        // renderRulePage is called in sorted order: A, B, C
        when(templateRenderer.renderRulePage(ruleReportModelCaptor.capture()))
                .thenReturn(RULE_PAGE_CONTENT_A, RULE_PAGE_CONTENT_B, RULE_PAGE_CONTENT_C);
        when(confluenceAdapter.addOrUpdatePageUnderAncestor(RULES_PAGE_ID, "Rule A", RULE_PAGE_CONTENT_A)).thenReturn(RULE_PAGE_ID_A);
        when(confluenceAdapter.addOrUpdatePageUnderAncestor(RULES_PAGE_ID, "Rule B", RULE_PAGE_CONTENT_B)).thenReturn(RULE_PAGE_ID_B);
        when(confluenceAdapter.addOrUpdatePageUnderAncestor(RULES_PAGE_ID, "Rule C", RULE_PAGE_CONTENT_C)).thenReturn(RULE_PAGE_ID_C);

        reportGenerator.generateRulesReport(List.of(ruleB, ruleA, ruleC), true);

        // Verify the RulesReportModel passed to the template renderer
        RulesReportModel capturedRulesModel = rulesReportModelCaptor.getValue();
        assertEquals("Rules", capturedRulesModel.getTitle());
        assertEquals(3, capturedRulesModel.getRules().size());

        // Verify rules were sorted alphabetically
        List<RuleReportModel> capturedRules = ruleReportModelCaptor.getAllValues();
        assertEquals(3, capturedRules.size());
        assertEquals("Rule A", capturedRules.get(0).getName());
        assertEquals("Rule B", capturedRules.get(1).getName());
        assertEquals("Rule C", capturedRules.get(2).getName());

        // Verify orphan cleanup includes all generated pages
        verify(confluenceAdapter).deleteOrphanPages(
                RULES_PAGE_ID,
                Set.of(RULE_PAGE_ID_A, RULE_PAGE_ID_B, RULE_PAGE_ID_C)
        );
    }

    @Test
    void testGenerateRulesReport_withoutOrphanCleanup() {
        when(properties.getConfluenceRootPageName()).thenReturn(ROOT_PAGE_NAME);
        when(confluenceAdapter.getPageByName(ROOT_PAGE_NAME)).thenReturn(ROOT_PAGE_ID);

        // Input is unsorted: B, A, C — expect alphabetical output: A, B, C
        ReportingRule ruleB = createReportingRule("rule-2", "Rule B");
        ReportingRule ruleA = createReportingRule("rule-1", "Rule A");
        ReportingRule ruleC = createReportingRule("rule-3", "Rule C");

        ArgumentCaptor<RulesReportModel> rulesReportModelCaptor = ArgumentCaptor.forClass(RulesReportModel.class);
        when(templateRenderer.renderRulesPage(rulesReportModelCaptor.capture())).thenReturn(RULES_PAGE_CONTENT);
        when(confluenceAdapter.addOrUpdatePageUnderAncestor(ROOT_PAGE_ID, "Rules", RULES_PAGE_CONTENT)).thenReturn(RULES_PAGE_ID);

        ArgumentCaptor<RuleReportModel> ruleReportModelCaptor = ArgumentCaptor.forClass(RuleReportModel.class);
        // renderRulePage is called in sorted order: A, B, C
        when(templateRenderer.renderRulePage(ruleReportModelCaptor.capture()))
                .thenReturn(RULE_PAGE_CONTENT_A, RULE_PAGE_CONTENT_B, RULE_PAGE_CONTENT_C);
        when(confluenceAdapter.addOrUpdatePageUnderAncestor(RULES_PAGE_ID, "Rule A", RULE_PAGE_CONTENT_A)).thenReturn(RULE_PAGE_ID_A);
        when(confluenceAdapter.addOrUpdatePageUnderAncestor(RULES_PAGE_ID, "Rule B", RULE_PAGE_CONTENT_B)).thenReturn(RULE_PAGE_ID_B);
        when(confluenceAdapter.addOrUpdatePageUnderAncestor(RULES_PAGE_ID, "Rule C", RULE_PAGE_CONTENT_C)).thenReturn(RULE_PAGE_ID_C);

        reportGenerator.generateRulesReport(List.of(ruleB, ruleA, ruleC), false);

        // Verify the RulesReportModel passed to the template renderer
        RulesReportModel capturedRulesModel = rulesReportModelCaptor.getValue();
        assertEquals("Rules", capturedRulesModel.getTitle());
        assertEquals(3, capturedRulesModel.getRules().size());

        // Verify rules were sorted alphabetically
        List<RuleReportModel> capturedRules = ruleReportModelCaptor.getAllValues();
        assertEquals(3, capturedRules.size());
        assertEquals("Rule A", capturedRules.get(0).getName());
        assertEquals("Rule B", capturedRules.get(1).getName());
        assertEquals("Rule C", capturedRules.get(2).getName());

        // Verify orphan cleanup includes all generated pages
        verify(confluenceAdapter, never()).deleteOrphanPages(anyString(), anySet());
    }


    private ReportingSystemScore createSystemScore(String systemName, int score) {
        ReportingSystemScore systemScore = mock(ReportingSystemScore.class);
        ReportingScore score1 = mock(ReportingScore.class);
        ReportingScore score2 = mock(ReportingScore.class);

        ReportingRuleState ruleState = mock(ReportingRuleState.class);
        when(ruleState.state()).thenReturn(State.OK);

        ReportingComponentScore componentScore1 = mock(ReportingComponentScore.class);
        when(componentScore1.getComponentName()).thenReturn(systemName + " Component A");
        when(componentScore1.getLatestScore()).thenReturn(score - 10);
        when(componentScore1.getScoringTrend()).thenReturn(TrendIndicator.DOWN);
        when(componentScore1.getRuleStates()).thenReturn(List.of(ruleState));

        when(systemScore.getSystemName()).thenReturn(systemName);
        when(systemScore.getScores()).thenReturn(List.of(score1, score2));
        when(systemScore.getLatestScore()).thenReturn(score);
        when(systemScore.getScoringTrend()).thenReturn(TrendIndicator.UP);
        when(systemScore.getComponentScores()).thenReturn(List.of(componentScore1));

        return systemScore;
    }

    private ReportingRule createReportingRule(String ruleId, String ruleName) {
        ReportingRule rule = mock(ReportingRule.class);
        when(rule.getRuleId()).thenReturn(ruleId);
        when(rule.getRuleName()).thenReturn(ruleName);
        when(rule.getConformanceRateTrend()).thenReturn(TrendIndicator.UP);
        when(rule.getSystemConformanceRates()).thenReturn(List.of());
        when(rule.getNonCompliantComponents()).thenReturn(List.of());
        return rule;
    }

}
