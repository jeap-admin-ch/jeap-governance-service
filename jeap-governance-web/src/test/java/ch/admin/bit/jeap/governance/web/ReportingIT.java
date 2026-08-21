package ch.admin.bit.jeap.governance.web;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleInfo;
import ch.admin.bit.jeap.governance.domain.rule.RuleConformanceRate;
import ch.admin.bit.jeap.governance.domain.rule.RuleId;
import ch.admin.bit.jeap.governance.domain.rule.RuleRepository;
import ch.admin.bit.jeap.governance.domain.rule.RuleState;
import ch.admin.bit.jeap.governance.domain.rule.State;
import ch.admin.bit.jeap.governance.domain.rule.SystemRuleConformanceRate;
import ch.admin.bit.jeap.governance.domain.score.ComponentScore;
import ch.admin.bit.jeap.governance.domain.score.SystemScore;
import ch.admin.bit.jeap.governance.reporting.ReportingScheduler;
import ch.admin.bit.jeap.governance.reporting.confluence.ConfluenceAdapter;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@SpringBootTest(properties = {
        "jeap.governance.reporting.enabled=true",
        "jeap.governance.reporting.confluence.url=http://localhost:8090",
        "jeap.governance.reporting.confluence.space-key=dummy",
        "jeap.governance.reporting.confluence.root-page-name=dummy",
        "jeap.governance.reporting.confluence.ancestor-id=dummy",
        "jeap.governance.reporting.confluence.username=dummy",
        "jeap.governance.reporting.confluence.password=dummy",
        "jeap.governance.reporting.lock-at-least=10ms",
        "jeap.governance.reporting.lock-at-most=20ms",
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Transactional
class ReportingIT extends GovernanceIntegrationTestBase {

    @Autowired
    private ReportingScheduler reportingScheduler;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private ConfluenceAdapter confluenceAdapter;

    @MockitoBean
    private RuleRepository ruleRepository;

    @Test
    void generateDocumentation() {
        when(confluenceAdapter.addOrUpdatePageUnderAncestor(any(), eq("System Scores"), any())).thenReturn("systems-scores-page-id");
        when(confluenceAdapter.addOrUpdatePageUnderAncestor(any(), eq("System 1 (System scores)"), any())).thenReturn("systems-score1-page-id");
        when(confluenceAdapter.addOrUpdatePageUnderAncestor(any(), eq("Component 1 (Component scores)"), any())).thenReturn("component-score1-page-id");
        when(confluenceAdapter.addOrUpdatePageUnderAncestor(any(), eq("Component 2 (Component scores)"), any())).thenReturn("component-score2-page-id");
        when(confluenceAdapter.addOrUpdatePageUnderAncestor(any(), eq("Rules"), any())).thenReturn("rules-page-id");
        when(confluenceAdapter.addOrUpdatePageUnderAncestor(any(), eq("Rule 1"), any())).thenReturn("rule1-page-id");
        when(confluenceAdapter.addOrUpdatePageUnderAncestor(any(), eq("Rule 2"), any())).thenReturn("rule2-page-id");
        createAndPersistDefaultModel();

        reportingScheduler.generateDocumentation();

        verify(confluenceAdapter).addOrUpdatePageUnderAncestor(any(), eq("System Scores"), any());
        verify(confluenceAdapter).addOrUpdatePageUnderAncestor(any(), eq("System 1 (System scores)"), any());
        verify(confluenceAdapter).addOrUpdatePageUnderAncestor(any(), eq("Component 1 (Component scores)"), any());
        verify(confluenceAdapter).addOrUpdatePageUnderAncestor(any(), eq("Component 2 (Component scores)"), any());
        verify(confluenceAdapter).addOrUpdatePageUnderAncestor(any(), eq("Rules"), any());
        verify(confluenceAdapter).addOrUpdatePageUnderAncestor(any(), eq("Rule 1"), any());
        verify(confluenceAdapter).addOrUpdatePageUnderAncestor(any(), eq("Rule 2"), any());
        verify(confluenceAdapter).addOrUpdatePageUnderAncestor(any(), eq("Rule 2"), argThat(content ->
                content.contains("Components in Violation Grace Period") &&
                        content.contains("Component 1") &&
                        content.contains("Comment") &&
                        content.contains("Outdated message contracts:") &&
                        content.contains("FirstEvent uses 1.0.0, latest is 2.0.0") &&
                        content.contains("Violation Detected") &&
                        content.contains("Grace Period Ends")));
        verify(confluenceAdapter, never()).deleteOrphanPages(anyString(), anySet());
    }

    @Test
    void generateDocumentationWithOrphanCleanup() {
        when(confluenceAdapter.addOrUpdatePageUnderAncestor(any(), eq("System Scores"), any())).thenReturn("systems-scores-page-id");
        when(confluenceAdapter.addOrUpdatePageUnderAncestor(any(), eq("System 1 (System scores)"), any())).thenReturn("systems-score1-page-id");
        when(confluenceAdapter.addOrUpdatePageUnderAncestor(any(), eq("Component 1 (Component scores)"), any())).thenReturn("component-score1-page-id");
        when(confluenceAdapter.addOrUpdatePageUnderAncestor(any(), eq("Component 2 (Component scores)"), any())).thenReturn("component-score2-page-id");
        when(confluenceAdapter.addOrUpdatePageUnderAncestor(any(), eq("Rules"), any())).thenReturn("rules-page-id");
        when(confluenceAdapter.addOrUpdatePageUnderAncestor(any(), eq("Rule 1"), any())).thenReturn("rule1-page-id");
        when(confluenceAdapter.addOrUpdatePageUnderAncestor(any(), eq("Rule 2"), any())).thenReturn("rule2-page-id");
        createAndPersistDefaultModel();

        reportingScheduler.generateDocumentationWithOrphanCleanup();

        verify(confluenceAdapter).addOrUpdatePageUnderAncestor(any(), eq("System Scores"), any());
        verify(confluenceAdapter).addOrUpdatePageUnderAncestor(any(), eq("System 1 (System scores)"), any());
        verify(confluenceAdapter).addOrUpdatePageUnderAncestor(any(), eq("Component 1 (Component scores)"), any());
        verify(confluenceAdapter).addOrUpdatePageUnderAncestor(any(), eq("Component 2 (Component scores)"), any());
        verify(confluenceAdapter).addOrUpdatePageUnderAncestor(any(), eq("Rules"), any());
        verify(confluenceAdapter).addOrUpdatePageUnderAncestor(any(), eq("Rule 1"), any());
        verify(confluenceAdapter).addOrUpdatePageUnderAncestor(any(), eq("Rule 2"), any());

        verify(confluenceAdapter).deleteOrphanPages("systems-scores-page-id", Set.of("systems-score1-page-id", "component-score1-page-id", "component-score2-page-id"));
        verify(confluenceAdapter).deleteOrphanPages("rules-page-id", Set.of("rule1-page-id", "rule2-page-id"));
    }

    private void createAndPersistDefaultModel() {
        SystemComponent systemComponent1 = SystemComponent.builder()
                .name("Component 1")
                .type(ComponentType.BACKEND_SERVICE)
                .build();

        SystemComponent systemComponent2 = SystemComponent.builder()
                .name("Component 2")
                .type(ComponentType.SELF_CONTAINED_SYSTEM)
                .build();

        System system = System.builder()
                .name("System 1")
                .systemComponents(List.of(systemComponent1, systemComponent2))
                .aliases(Set.of())
                .build();
        entityManager.persist(system);

        SystemScore score1 = SystemScore.builder()
                .system(system)
                .score(80)
                .day(LocalDate.now().minusDays(2))
                .build();
        SystemScore score2 = SystemScore.builder()
                .system(system)
                .score(77)
                .day(LocalDate.now().minusDays(1))
                .build();
        SystemScore score3 = SystemScore.builder()
                .system(system)
                .score(77)
                .day(LocalDate.now())
                .build();

        entityManager.persist(score1);
        entityManager.persist(score2);
        entityManager.persist(score3);

        ComponentScore componentScore11 = ComponentScore.builder()
                .systemComponent(systemComponent1)
                .score(90)
                .day(LocalDate.now().minusDays(2))
                .build();
        ComponentScore componentScore12 = ComponentScore.builder()
                .systemComponent(systemComponent1)
                .score(90)
                .day(LocalDate.now().minusDays(1))
                .build();
        ComponentScore componentScore13 = ComponentScore.builder()
                .systemComponent(systemComponent1)
                .score(90)
                .day(LocalDate.now())
                .build();

        ComponentScore componentScore21 = ComponentScore.builder()
                .systemComponent(systemComponent2)
                .score(90)
                .day(LocalDate.now())
                .build();

        entityManager.persist(componentScore11);
        entityManager.persist(componentScore12);
        entityManager.persist(componentScore13);
        entityManager.persist(componentScore21);

        RuleInfo ruleInfo1 = new RuleInfo(RuleId.of("rule1"), "Rule 1", "http://documentation-link-for-rule-1");
        RuleInfo ruleInfo2 = new RuleInfo(RuleId.of("rule2"), "Rule 2", "http://documentation-link-for-rule-2",
                Duration.ofDays(7));

        when(ruleRepository.getActiveRuleInfos()).thenReturn(List.of(ruleInfo1, ruleInfo2));

        RuleConformanceRate conformanceRate1 = RuleConformanceRate.builder()
                .ruleId("rule1")
                .conformanceRate(80)
                .day(LocalDate.now().minusDays(1))
                .build();
        RuleConformanceRate conformanceRate2 = RuleConformanceRate.builder()
                .ruleId("rule2")
                .conformanceRate(80)
                .day(LocalDate.now().minusDays(1))
                .build();

        entityManager.persist(conformanceRate1);
        entityManager.persist(conformanceRate2);

        SystemRuleConformanceRate systemRuleConformanceRate1 = SystemRuleConformanceRate.builder()
                .systemId(system.getId())
                .ruleId("rule1")
                .conformanceRate(80)
                .day(LocalDate.now().minusDays(1))
                .build();
        SystemRuleConformanceRate systemRuleConformanceRate2 = SystemRuleConformanceRate.builder()
                .systemId(system.getId())
                .ruleId("rule2")
                .conformanceRate(70)
                .day(LocalDate.now())
                .build();

        entityManager.persist(systemRuleConformanceRate1);
        entityManager.persist(systemRuleConformanceRate2);

        RuleState ruleState1 = RuleState.builder()
                .systemComponent(systemComponent1)
                .ruleId(RuleId.of("rule1"))
                .state(State.FAIL)
                .build();
        ZonedDateTime now = ZonedDateTime.now();
        RuleState ruleState2 = RuleState.builder()
                .systemComponent(systemComponent1)
                .ruleId(RuleId.of("rule2"))
                .state(State.OK)
                .ruleStateComment("Outdated message contracts:\nFirstEvent uses 1.0.0, latest is 2.0.0")
                .build();
        setField(ruleState2, "violationDetectedAt", now.minusDays(1));

        entityManager.persist(ruleState1);
        entityManager.persist(ruleState2);

        entityManager.flush();
    }

    @BeforeEach
    @Transactional
    void cleanDatabase() {
        entityManager.createNativeQuery("DELETE FROM rule_state").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM rule_conformance_rate").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM system_rule_conformance_rate").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM component_score").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM system_score").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM system_component").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM system").executeUpdate();
    }
}
